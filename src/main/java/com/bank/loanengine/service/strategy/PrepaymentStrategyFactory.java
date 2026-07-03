package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the {@link PrepaymentStrategy} bean that implements a given {@link BusinessOption}.
 * Spring auto-wires every {@code PrepaymentStrategy} bean present on the classpath into the
 * list below, so adding a new option in future only requires creating a new
 * {@code @Component} class - this factory and the calling service never need to change.
 */
@Component
public class PrepaymentStrategyFactory {

    private final Map<BusinessOption, PrepaymentStrategy> strategiesByOption;

    public PrepaymentStrategyFactory(List<PrepaymentStrategy> strategies) {
        this.strategiesByOption = strategies.stream()
                .collect(Collectors.toMap(PrepaymentStrategy::supportedOption, Function.identity()));
    }

    public PrepaymentStrategy resolve(BusinessOption option) {
        PrepaymentStrategy strategy = strategiesByOption.get(option);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No prepayment strategy is implemented for option: " + option
                            + ". This submission implements Category A only (REDUCE_EMI_KEEP_TENOR, "
                            + "REDUCE_TENOR_KEEP_EMI, ADVANCE_INSTALLMENTS).");
        }
        return strategy;
    }
}
