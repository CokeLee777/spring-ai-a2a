package io.github.cokelee777.agent.host.memory;

import java.util.List;

/**
 * Long-term memory service.
 *
 * <p>
 * Retrieves relevant knowledge for the given actor via semantic search. Long-term records
 * are populated asynchronously by a memory strategy when short-term events accumulate.
 * </p>
 */
public interface LongTermMemoryService {

	/**
	 * Returns relevant long-term memory records for the actor matching the search query.
	 * Returns an empty list when no strategy is configured or mode does not use
	 * long-term.
	 * @param actorId the actor identifier
	 * @param searchQuery the query derived from the current user prompt
	 * @return list of relevant text snippets
	 */
	List<String> retrieveRelevant(String actorId, String searchQuery);

}
