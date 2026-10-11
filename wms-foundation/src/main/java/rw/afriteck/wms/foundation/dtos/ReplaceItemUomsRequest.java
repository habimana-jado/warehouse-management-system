package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceItemUomsRequest(@NotNull @Valid List<ItemUomRequest> uoms) {}

