package com.namehider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

/** Пытается заменять ваш ник в строках лога (latest.log и консоль). */
public final class NHLogFilter extends AbstractFilter {

    public static void install() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration cfg = ctx.getConfiguration();
            NHLogFilter f = new NHLogFilter();
            cfg.getRootLogger().addFilter(f);
            for (LoggerConfig lc : cfg.getLoggers().values()) lc.addFilter(f);
        } catch (Throwable t) {
            // логи останутся как есть
        }
    }

    @Override
    public Result filter(LogEvent event) {
        try {
            if (event instanceof MutableLogEvent m && m.getMessage() != null) {
                String s = m.getMessage().getFormattedMessage();
                String r = NHText.replaceOwnPlain(s);
                if (r != null && !r.equals(s)) m.setMessage(new SimpleMessage(r));
            }
        } catch (Throwable ignored) {
        }
        return Result.NEUTRAL;
    }
}
