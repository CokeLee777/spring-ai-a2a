package io.github.cokelee777.agentcore.autoconfigure.controller;

import com.google.gson.JsonSyntaxException;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.jsonrpc.common.json.IdJsonMappingException;
import io.a2a.jsonrpc.common.json.InvalidParamsJsonMappingException;
import io.a2a.jsonrpc.common.json.JsonMappingException;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import io.a2a.jsonrpc.common.wrappers.A2AErrorResponse;
import io.a2a.jsonrpc.common.wrappers.A2ARequest;
import io.a2a.jsonrpc.common.wrappers.A2AResponse;
import io.a2a.jsonrpc.common.wrappers.CancelTaskRequest;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksRequest;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.jsonrpc.common.wrappers.NonStreamingJSONRPCRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.A2AError;
import io.a2a.spec.EventKind;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.UnsupportedOperationError;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * REST controller handling A2A Protocol JSON-RPC requests at {@code POST /}.
 *
 * <p>
 * Incoming request bodies are parsed via {@link JSONRPCUtils#parseRequestBody} and
 * dispatched to the appropriate {@link RequestHandler} method. Error conditions produce
 * JSON-RPC error responses.
 * </p>
 */
@RestController
public class A2AJsonRpcController {

	private final RequestHandler requestHandler;

	/**
	 * Create a new {@link A2AJsonRpcController}.
	 * @param requestHandler the request handler dispatching A2A JSON-RPC calls
	 */
	public A2AJsonRpcController(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	/**
	 * Handles a synchronous A2A JSON-RPC request and returns the serialized response.
	 * @param body the raw JSON-RPC request body
	 * @return {@code 200 OK} with the serialized JSON-RPC result, or {@code 500} with a
	 * JSON-RPC error response if parsing or dispatch fails
	 */
	@PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> handle(@RequestBody String body) {
		A2ARequest<?> request = null;
		A2AErrorResponse error = null;

		try {
			ServerCallContext context = createCallContext();
			request = JSONRPCUtils.parseRequestBody(body, null);
			if (request instanceof NonStreamingJSONRPCRequest<?> nonStreamingRequest) {
				A2AResponse<?> response = processNonStreamingRequest(nonStreamingRequest, context);
				return ResponseEntity.ok(serializeResponse(response));
			}
		}
		catch (A2AError e) {
			error = new A2AErrorResponse(e);
		}
		catch (InvalidParamsJsonMappingException e) {
			error = new A2AErrorResponse(e.getId(), new io.a2a.spec.InvalidParamsError(null, e.getMessage(), null));
		}
		catch (MethodNotFoundJsonMappingException e) {
			error = new A2AErrorResponse(e.getId(), new io.a2a.spec.MethodNotFoundError(null, e.getMessage(), null));
		}
		catch (IdJsonMappingException e) {
			error = new A2AErrorResponse(e.getId(), new io.a2a.spec.InvalidRequestError(null, e.getMessage(), null));
		}
		catch (JsonMappingException e) {
			error = new A2AErrorResponse(new io.a2a.spec.InvalidRequestError(null, e.getMessage(), null));
		}
		catch (JsonSyntaxException | JsonProcessingException e) {
			error = new A2AErrorResponse(new JSONParseError(e.getMessage()));
		}
		catch (Throwable t) {
			error = new A2AErrorResponse(new InternalError(t.getMessage()));
		}

		if (error != null) {
			return ResponseEntity.internalServerError().body(serializeResponse(error));
		}

		return ResponseEntity.internalServerError()
			.body(serializeResponse(generateErrorResponse(request, new UnsupportedOperationError())));
	}

	private ServerCallContext createCallContext() {
		return new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
	}

	private A2AResponse<?> processNonStreamingRequest(NonStreamingJSONRPCRequest<?> request,
			ServerCallContext context) {
		Object requestId = request.getId();
		if (request instanceof GetTaskRequest req) {
			Task task = requestHandler.onGetTask(req.getParams(), context);
			return new GetTaskResponse(requestId, task);
		}
		if (request instanceof CancelTaskRequest req) {
			Task task = requestHandler.onCancelTask(req.getParams(), context);
			return new CancelTaskResponse(requestId, task);
		}
		if (request instanceof ListTasksRequest req) {
			ListTasksResult listTasksResult = requestHandler.onListTasks(req.getParams(), context);
			return new ListTasksResponse(requestId, listTasksResult);
		}
		if (request instanceof CreateTaskPushNotificationConfigRequest req) {
			TaskPushNotificationConfig config = requestHandler.onCreateTaskPushNotificationConfig(req.getParams(),
					context);
			return new CreateTaskPushNotificationConfigResponse(requestId, config);
		}
		if (request instanceof GetTaskPushNotificationConfigRequest req) {
			TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(req.getParams(),
					context);
			return new GetTaskPushNotificationConfigResponse(requestId, config);
		}
		if (request instanceof SendMessageRequest req) {
			EventKind result = requestHandler.onMessageSend(req.getParams(), context);
			return new SendMessageResponse(requestId, result);
		}
		if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
			requestHandler.onDeleteTaskPushNotificationConfig(req.getParams(), context);
			return new DeleteTaskPushNotificationConfigResponse(request.getId());
		}
		return generateErrorResponse(request, new UnsupportedOperationError());
	}

	private A2AResponse<?> generateErrorResponse(A2ARequest<?> request, A2AError error) {
		return new A2AErrorResponse(request.getId(), error);
	}

	private static String serializeResponse(A2AResponse<?> response) {
		if (response instanceof A2AErrorResponse err) {
			return JSONRPCUtils.toJsonRPCErrorResponse(err.getId(), err.getError());
		}
		if (response.getError() != null) {
			return JSONRPCUtils.toJsonRPCErrorResponse(response.getId(), response.getError());
		}
		com.google.protobuf.MessageOrBuilder protoMessage = convertToProto(response);
		return JSONRPCUtils.toJsonRPCResultResponse(response.getId(), protoMessage);
	}

	private static com.google.protobuf.MessageOrBuilder convertToProto(A2AResponse<?> response) {
		if (response instanceof GetTaskResponse r) {
			return ProtoUtils.ToProto.task(r.getResult());
		}
		else if (response instanceof CancelTaskResponse r) {
			return ProtoUtils.ToProto.task(r.getResult());
		}
		else if (response instanceof SendMessageResponse r) {
			return ProtoUtils.ToProto.taskOrMessage(r.getResult());
		}
		else if (response instanceof ListTasksResponse r) {
			return ProtoUtils.ToProto.listTasksResult(r.getResult());
		}
		else if (response instanceof CreateTaskPushNotificationConfigResponse r) {
			return ProtoUtils.ToProto.createTaskPushNotificationConfigResponse(r.getResult());
		}
		else if (response instanceof GetTaskPushNotificationConfigResponse r) {
			return ProtoUtils.ToProto.getTaskPushNotificationConfigResponse(r.getResult());
		}
		else if (response instanceof DeleteTaskPushNotificationConfigResponse) {
			return com.google.protobuf.Empty.getDefaultInstance();
		}
		else if (response instanceof GetExtendedAgentCardResponse r) {
			return ProtoUtils.ToProto.getExtendedCardResponse(r.getResult());
		}
		else if (response instanceof SendStreamingMessageResponse r) {
			return ProtoUtils.ToProto.taskOrMessageStream(r.getResult());
		}
		else {
			throw new IllegalArgumentException("Unknown response type: " + response.getClass().getName());
		}
	}

}
