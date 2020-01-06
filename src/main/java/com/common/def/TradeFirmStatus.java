package com.common.def;


public enum TradeFirmStatus implements ServiceData
{
    ACTIVE("active"), INACTIVE("inactive");

    private String code;

    private TradeFirmStatus(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    private static final EnumLookerUpper<TradeFirmStatus> HELPER = new EnumLookerUpper<>(TradeFirmStatus.class);

    @FactoryMethod
    public static TradeFirmStatus valueOfCode(String key){
        return HELPER.lookup(key);
    }
}
