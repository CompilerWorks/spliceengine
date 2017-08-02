package com.splicemachine.customer;

import com.splicemachine.db.shared.common.sqlj.SysfunProvider;

public class CustomerSysfuns implements SysfunProvider {
    public static double CUSTOMER_TRUNC(double x, int n) {
        double pow10n = Math.pow(10, n);
        if (x >= 0) {
            return Math.floor(pow10n * x) / pow10n;
        } else {
            return Math.ceil(pow10n * x) / pow10n;
        }
    }
}
