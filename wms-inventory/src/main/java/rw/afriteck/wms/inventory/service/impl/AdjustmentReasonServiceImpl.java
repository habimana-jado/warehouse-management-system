package rw.afriteck.wms.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;
import rw.afriteck.wms.inventory.mapper.AdjustmentReasonMapper;
import rw.afriteck.wms.inventory.repository.AdjustmentReasonRepository;
import rw.afriteck.wms.inventory.service.AdjustmentReasonService;
import rw.afriteck.wms.common.support.PageRules;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdjustmentReasonServiceImpl implements AdjustmentReasonService {

    private final AdjustmentReasonRepository reasons;

    @Override
    @Transactional
    public AdjustmentReasonResponse create(CreateAdjustmentReasonRequest r) {
        String code = r.code() == null ? null : Codes.blankToNull(Codes.normalize(r.code()));
        String name = Codes.blankToNull(r.name());
        if (code == null || name == null) {
            throw new BusinessException("REASON_FIELDS_REQUIRED", "Code and name are required");
        }
        if (reasons.existsByCode(code)) {
            throw new ConflictException("REASON_CODE_EXISTS", "Reason code already exists: " + code);
        }
        AdjustmentReason reason = new AdjustmentReason();
        reason.setCode(code);
        reason.setName(name);
        reason.setDirection(r.direction() != null ? r.direction() : AdjustmentDirection.BOTH);
        reason.setRequiresApproval(r.requiresApproval() == null || r.requiresApproval());
        return AdjustmentReasonMapper.toResponse(reasons.saveAndFlush(reason));
    }

    @Override
    @Transactional
    public AdjustmentReasonResponse update(UUID id, UpdateAdjustmentReasonRequest r) {
        AdjustmentReason reason = reasons.findById(id)
                .orElseThrow(() -> new NotFoundException("AdjustmentReason", id));
        if (r.name() != null) {
            String name = Codes.blankToNull(r.name());
            if (name == null) {
                throw new BusinessException("REASON_FIELDS_REQUIRED", "Name cannot be blank");
            }
            reason.setName(name);
        }
        if (r.direction() != null) reason.setDirection(r.direction());
        if (r.requiresApproval() != null) reason.setRequiresApproval(r.requiresApproval());
        if (r.active() != null) reason.setActive(r.active());
        return AdjustmentReasonMapper.toResponse(reasons.saveAndFlush(reason));
    }

    @Override
    @Transactional(readOnly = true)
    public AdjustmentReasonResponse getById(UUID id) {
        return reasons.findById(id).map(AdjustmentReasonMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("AdjustmentReason", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdjustmentReasonResponse> search(String q, boolean includeInactive, Pageable pageable) {
        Pageable p = PageRules.sanitize(pageable, Set.of("code", "name"), Sort.by("name"));
        return PageResponse.of(reasons.search(Codes.likePattern(q), includeInactive, p)
                .map(AdjustmentReasonMapper::toResponse));
    }
}
