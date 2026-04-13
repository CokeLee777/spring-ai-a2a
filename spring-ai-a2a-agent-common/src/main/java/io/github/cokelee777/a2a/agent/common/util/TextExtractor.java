package io.github.cokelee777.a2a.agent.common.util;

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

import java.util.stream.Collectors;

/**
 * Utility class for extracting plain text from A2A protocol objects.
 *
 * <p>
 * All methods are static; this class is not meant to be instantiated.
 * </p>
 */
public final class TextExtractor {

	private TextExtractor() {
	}

	/**
	 * Extracts and concatenates text content from all {@link TextPart} parts of the given
	 * {@link Message}.
	 * @param message the A2A message whose parts to extract
	 * @return the trimmed concatenated text, or an empty string if the message is null or
	 * has no text parts
	 */
	public static String extractTextFromMessage(Message message) {
		if (message == null || message.getParts() == null) {
			return "";
		}
		return message.getParts()
			.stream()
			.filter(part -> part instanceof TextPart)
			.map(part -> ((TextPart) part).getText())
			.collect(Collectors.joining())
			.trim();
	}

	/**
	 * Extracts and concatenates text content from all {@link TextPart} parts of the given
	 * {@link Artifact}.
	 * @param artifact the A2A artifact whose parts to extract
	 * @return the concatenated text, or an empty string if the artifact is null or has no
	 * text parts
	 */
	public static String extractTextFromArtifact(Artifact artifact) {
		if (artifact == null || artifact.parts() == null) {
			return "";
		}
		return artifact.parts()
			.stream()
			.filter(part -> part instanceof TextPart)
			.map(part -> ((TextPart) part).getText())
			.collect(Collectors.joining());
	}

	/**
	 * Concatenates the text of all {@link TextPart} parts across every artifact in the
	 * given {@link Task}.
	 * @param task the A2A task whose artifacts to extract
	 * @return the concatenated text, or an empty string if the task has no artifacts or
	 * text parts
	 */
	public static String extractTextFromTask(Task task) {
		if (task == null || task.getArtifacts() == null) {
			return "";
		}
		return task.getArtifacts().stream().map(TextExtractor::extractTextFromArtifact).collect(Collectors.joining());
	}

}
