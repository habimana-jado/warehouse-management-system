package rw.afriteck.wms.operations.service;

import rw.afriteck.wms.operations.dtos.ProductivityFilter;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;

import java.util.List;

public interface TaskProductivityService {
    List<WorkerProductivityResponse> byWorker(ProductivityFilter filter);
}
