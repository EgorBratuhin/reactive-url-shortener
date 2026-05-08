package by.bratukhin.shortener.support;

import java.util.List;

///
/// Paginated list of items with cursor-based pagination metadata.
///
/// @param items      list of items in the current page
/// @param hasNext    whether there is a next page available
/// @param nextCursor cursor for retrieving the next page
///
public record ItemsPage<T>(List<T> items, boolean hasNext, String nextCursor) {

}
