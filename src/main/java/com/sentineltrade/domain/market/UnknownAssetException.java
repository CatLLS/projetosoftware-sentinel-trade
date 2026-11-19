package com.sentineltrade.domain.market;

import com.sentineltrade.domain.DomainException;

public class UnknownAssetException extends DomainException {

    public UnknownAssetException() {
        super("Asset is not available for trading");
    }
}
