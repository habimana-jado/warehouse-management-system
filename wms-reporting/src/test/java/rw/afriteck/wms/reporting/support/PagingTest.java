package rw.afriteck.wms.reporting.support;

import org.junit.jupiter.api.Test;
import rw.afriteck.wms.common.dtos.PageResponse;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PagingTest {

    @Test
    void sizeAndPageNormalization() {
        assertEquals(50, Paging.size(0));
        assertEquals(50, Paging.size(-5));
        assertEquals(20, Paging.size(20));
        assertEquals(100, Paging.size(150));

        assertEquals(0, Paging.page(-1));
        assertEquals(0, Paging.page(0));
        assertEquals(5, Paging.page(5));
    }

    @Test
    void slice_firstPage() {
        List<Integer> list = IntStream.range(0, 25).boxed().toList();
        PageResponse<Integer> page = Paging.slice(list, 0, 10);

        assertEquals(0, page.page());
        assertEquals(10, page.size());
        assertEquals(25, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), page.content());
    }

    @Test
    void slice_outOfBoundsPage() {
        List<Integer> list = IntStream.range(0, 5).boxed().toList();
        PageResponse<Integer> page = Paging.slice(list, 2, 10);

        assertEquals(2, page.page());
        assertEquals(10, page.size());
        assertEquals(5, page.totalElements());
        assertEquals(1, page.totalPages());
        assertTrue(page.content().isEmpty());
    }

    @Test
    void totalPagesCalculation() {
        assertEquals(0, Paging.totalPages(0, 10));
        assertEquals(1, Paging.totalPages(5, 10));
        assertEquals(1, Paging.totalPages(10, 10));
        assertEquals(2, Paging.totalPages(11, 10));
    }
}
