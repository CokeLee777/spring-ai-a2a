package io.github.cokelee777.agent.common.util;

import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TextExtractor}.
 */
class TextExtractorTest {

	@Test
	void extractTextFromTask_nullArtifacts_returnsEmpty() {
		Task task = new Task("id", "ctx", new TaskStatus(TaskState.COMPLETED), null, null, null);

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEmpty();
	}

	@Test
	void extractTextFromTask_emptyArtifacts_returnsEmpty() {
		Task task = taskWithArtifacts(List.of());

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEmpty();
	}

	@Test
	void extractTextFromTask_singleTextPart_returnsText() {
		Task task = taskWithArtifacts(List.of(artifactWithParts(List.of(new TextPart("hello")))));

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEqualTo("hello");
	}

	@Test
	void extractTextFromTask_multipleTextParts_concatenatesAll() {
		Task task = taskWithArtifacts(List.of(artifactWithParts(List.of(new TextPart("foo"), new TextPart("bar")))));

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEqualTo("foobar");
	}

	@Test
	void extractTextFromTask_multipleArtifacts_concatenatesAcrossAll() {
		Task task = taskWithArtifacts(List.of(artifactWithParts(List.of(new TextPart("first "))),
				artifactWithParts(List.of(new TextPart("second")))));

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEqualTo("first second");
	}

	@Test
	void extractTextFromTask_nonTextPart_skipped() {
		Task task = taskWithArtifacts(List.of(artifactWithParts(List.of(new DataPart(Map.of("key", "value"))))));

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEmpty();
	}

	@Test
	void extractTextFromTask_mixedParts_onlyTextExtracted() {
		Task task = taskWithArtifacts(
				List.of(artifactWithParts(List.of(new TextPart("text"), new DataPart(Map.of("key", "value"))))));

		String result = TextExtractor.extractTextFromTask(task);

		assertThat(result).isEqualTo("text");
	}

	private static Task taskWithArtifacts(List<Artifact> artifacts) {
		return new Task("id", "ctx", new TaskStatus(TaskState.COMPLETED), artifacts, null, null);
	}

	private static Artifact artifactWithParts(List<Part<?>> parts) {
		return new Artifact("artifact-1", null, null, parts, null, null);
	}

}