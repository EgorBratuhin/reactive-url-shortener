package by.bratukhin.shortener.support;

import java.util.List;

///
/// Items page.
///
/// @param items      item list
/// @param hasNext    has next page
/// @param nextCursor next page cursor
///
public record ItemsPage<T>(List<T> items, boolean hasNext, String nextCursor) {

}
