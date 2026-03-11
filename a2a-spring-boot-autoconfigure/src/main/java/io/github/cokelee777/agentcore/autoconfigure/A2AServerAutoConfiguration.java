package io.github.cokelee777.agentcore.autoconfigure;

import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.github.cokelee777.agentcore.autoconfigure.controller.A2AJsonRpcController;
import io.github.cokelee777.agentcore.autoconfigure.properties.A2AServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration activated when an {@link AgentExecutor} bean is present.
 *
 * <p>
 * Wires up the full A2A server infrastructure: task store, event bus, push notification
 * support, thread pools, and the {@link DefaultRequestHandler}. Intended exclusively for
 * A2A server modules (e.g., order, delivery, payment agents), not orchestrators.
 * </p>
 */
@AutoConfiguration
@ConditionalOnBean(AgentExecutor.class)
@EnableConfigurationProperties(A2AServerProperties.class)
public class A2AServerAutoConfiguration {

	/**
	 * Create a new {@link A2AServerAutoConfiguration}.
	 */
	public A2AServerAutoConfiguration() {
	}

	/**
	 * Provides the A2A SDK configuration using built-in default values.
	 * @return a new {@link DefaultValuesConfigProvider}
	 */
	@Bean
	@ConditionalOnMissingBean
	public A2AConfigProvider configProvider() {
		return new DefaultValuesConfigProvider();
	}

	/**
	 * Provides an in-memory task store when no custom store bean is present.
	 * @return a new {@link InMemoryTaskStore}
	 */
	@Bean
	@ConditionalOnMissingBean
	public InMemoryTaskStore inMemoryTaskStore() {
		return new InMemoryTaskStore();
	}

	/**
	 * Provides an in-memory push-notification config store when none is present.
	 * @return a new {@link InMemoryPushNotificationConfigStore}
	 */
	@Bean
	@ConditionalOnMissingBean
	public PushNotificationConfigStore pushNotificationConfigStore() {
		return new InMemoryPushNotificationConfigStore();
	}

	/**
	 * Provides the push-notification sender backed by the given config store.
	 * @param pushNotificationConfigStore the config store for notification subscriptions
	 * @return a new {@link BasePushNotificationSender}
	 */
	@Bean
	@ConditionalOnMissingBean
	public BasePushNotificationSender pushNotificationSender(PushNotificationConfigStore pushNotificationConfigStore) {
		return new BasePushNotificationSender(pushNotificationConfigStore, A2AHttpClientFactory.create());
	}

	/**
	 * Provides the central event bus when none is present.
	 * @return a new {@link MainEventBus}
	 */
	@Bean
	@ConditionalOnMissingBean
	public MainEventBus mainEventBus() {
		return new MainEventBus();
	}

	/**
	 * Provides the queue manager that bridges the task store and event bus.
	 * @param taskStore the in-memory task store
	 * @param mainEventBus the central event bus
	 * @return a new {@link InMemoryQueueManager}
	 */
	@Bean
	@ConditionalOnMissingBean
	public InMemoryQueueManager inMemoryQueueManager(InMemoryTaskStore taskStore, MainEventBus mainEventBus) {
		return new InMemoryQueueManager(taskStore, mainEventBus);
	}

	/**
	 * Provides the event-bus processor that drives task lifecycle transitions.
	 * @param mainEventBus the central event bus
	 * @param taskStore the task store to update
	 * @param pushNotificationSender sender for push-notification delivery
	 * @param queueManager the queue manager coordinating event flow
	 * @return a new {@link MainEventBusProcessor}
	 */
	@Bean
	@ConditionalOnMissingBean
	public MainEventBusProcessor mainEventBusProcessor(MainEventBus mainEventBus, InMemoryTaskStore taskStore,
			BasePushNotificationSender pushNotificationSender, InMemoryQueueManager queueManager) {
		return new MainEventBusProcessor(mainEventBus, taskStore, pushNotificationSender, queueManager);
	}

	/**
	 * Ensures {@link MainEventBusProcessor} is started when the Spring context starts.
	 * @param processor the processor to start
	 * @return a {@link SmartLifecycle} that calls {@code ensureStarted()} on refresh
	 */
	@Bean
	public SmartLifecycle mainEventBusProcessorLifecycle(MainEventBusProcessor processor) {
		return new SmartLifecycle() {
			@Override
			public void start() {
				processor.ensureStarted();
			}

			@Override
			public void stop() {
			}

			@Override
			public boolean isRunning() {
				return true;
			}
		};
	}

	/**
	 * Provides the thread pool used to execute {@link AgentExecutor} tasks.
	 * @param props A2A server configuration properties
	 * @return a {@link ThreadPoolExecutor} sized according to {@code props}
	 */
	@Bean
	@ConditionalOnMissingBean
	public ExecutorService agentExecutorService(A2AServerProperties props) {
		return new ThreadPoolExecutor(props.executorCorePoolSize(), props.executorMaxPoolSize(), 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(props.executorQueueCapacity()), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * Provides the thread pool used to consume events from the event bus.
	 * @param props A2A server configuration properties
	 * @return a {@link ThreadPoolExecutor} sized according to {@code props}
	 */
	@Bean
	@ConditionalOnMissingBean
	public ExecutorService eventConsumerExecutorService(A2AServerProperties props) {
		return new ThreadPoolExecutor(props.executorCorePoolSize(), props.executorMaxPoolSize(), 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(props.executorQueueCapacity()), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * Creates the {@link DefaultRequestHandler} that dispatches A2A JSON-RPC calls to the
	 * {@link AgentExecutor}.
	 * @param agentExecutor the application's agent executor
	 * @param taskStore the task store
	 * @param queueManager the queue manager
	 * @param pushNotificationConfigStore the push-notification config store
	 * @param processor the event-bus processor
	 * @param agentExecutorService thread pool for agent execution
	 * @param eventConsumerExecutorService thread pool for event consumption
	 * @return a fully wired {@link DefaultRequestHandler}
	 */
	@Bean
	@ConditionalOnMissingBean
	public DefaultRequestHandler requestHandler(AgentExecutor agentExecutor, InMemoryTaskStore taskStore,
			InMemoryQueueManager queueManager, PushNotificationConfigStore pushNotificationConfigStore,
			MainEventBusProcessor processor, ExecutorService agentExecutorService,
			ExecutorService eventConsumerExecutorService) {
		return DefaultRequestHandler.create(agentExecutor, taskStore, queueManager, pushNotificationConfigStore,
				processor, agentExecutorService, eventConsumerExecutorService);
	}

	/**
	 * Provides the JSON-RPC controller that handles all incoming A2A requests at
	 * {@code POST /}.
	 * @param requestHandler the fully wired request handler
	 * @return a new {@link A2AJsonRpcController}
	 */
	@Bean
	@ConditionalOnMissingBean
	public A2AJsonRpcController a2aJsonRpcController(DefaultRequestHandler requestHandler) {
		return new A2AJsonRpcController(requestHandler);
	}

}
