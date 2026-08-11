package com.euc.grantfit;

import java.util.List;

/** A nonprofit organization profile — one of the two inputs to a Grant Fit Assessment. */
public record Organization(
        String name,
        String missionStatement,
        List<String> programs,
        String operatingRegion,
        boolean isRegisteredNonprofit
) {}
