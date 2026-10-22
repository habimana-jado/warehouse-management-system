package rw.afriteck.wms.operations.support;

import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.enums.TaskType;

import java.util.EnumSet;
import java.util.Set;

public final class ClaimableTaskTypes {
    private ClaimableTaskTypes() {}

    public static Set<TaskType> forUser(UserContext ctx) {
        if (ctx.isAdmin() || ctx.hasRole("WAREHOUSE_MANAGER")) return EnumSet.allOf(TaskType.class);
        Set<TaskType> types = EnumSet.noneOf(TaskType.class);
        if (ctx.hasRole("RECEIVER")) types.addAll(EnumSet.of(TaskType.PUTAWAY, TaskType.TRANSFER, TaskType.COUNT, TaskType.OTHER));
        if (ctx.hasRole("PICKER"))   types.addAll(EnumSet.of(TaskType.PICK, TaskType.TRANSFER, TaskType.COUNT, TaskType.OTHER));
        return types;
    }
}
