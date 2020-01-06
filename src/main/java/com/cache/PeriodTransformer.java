package com.cache;
import com.util.TransformException;
import com.util.Transformer;

import java.util.concurrent.TimeUnit;

public class PeriodTransformer implements Transformer<String, Long>
{

    private TimeUnit targetUnit = null;
    private String regexSplitter = "\\s+";

    public static final PeriodTransformer MILLIS = new PeriodTransformer(TimeUnit.MILLISECONDS);
    public static final PeriodTransformer SECONDS = new PeriodTransformer(TimeUnit.SECONDS);

    public PeriodTransformer(TimeUnit timeUnit)
    {
        targetUnit = timeUnit;
    }

    @Override
    public Long transform(String period) throws TransformException
    {
        Long millis = null;
        try
        {
            if (period != null)
            {
                String[] parts = split(period);
                long duration = Long.parseLong(parts[0]);
                TimeUnit sourceUnit = targetUnit;
                if (parts.length == 2)
                {
                    sourceUnit = TimeUnit.valueOf(parts[1].toUpperCase());
                }

                millis = targetUnit.convert(duration, sourceUnit);
            }

            return millis;

        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error converting " + period + " into " + targetUnit, ex);
        }
    }

    protected String[] split(String period)
    {
        return period.split(regexSplitter, 2);
    }

    public TimeUnit getTargetUnit()
    {
        return targetUnit;
    }

    public void setTargetUnit(TimeUnit targetUnit)
    {
        this.targetUnit = targetUnit;
    }

    public String getRegexSplitter()
    {
        return regexSplitter;
    }

    public void setRegexSplitter(String regexSplitter)
    {
        this.regexSplitter = regexSplitter;
    }

}
