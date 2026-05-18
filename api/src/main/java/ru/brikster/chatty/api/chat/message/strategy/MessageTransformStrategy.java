package ru.brikster.chatty.api.chat.message.strategy;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import ru.brikster.chatty.api.chat.message.context.MessageContext;
import ru.brikster.chatty.api.chat.message.strategy.result.MessageTransformResult;

import static ru.brikster.chatty.api.chat.message.strategy.MessageTransformStrategy.TransformRule.*;

public interface MessageTransformStrategy<MessageT> {

    @NotNull MessageTransformResult<MessageT> handle(MessageContext<MessageT> context);

    @NotNull Stage getStage();

    /**
     * Actions a strategy is permitted to perform at a given {@link Stage}.
     * This is an allow-list: an action not listed for a stage is forbidden,
     * so newly added actions are denied by default until explicitly allowed.
     */
    enum TransformRule {
        ALLOW_CANCEL,
        ALLOW_FORMAT_UPDATE,
        ALLOW_UPDATE_RECIPIENTS
    }

    @Getter
    enum Stage {
        // Ungrouped stage with string message
        EARLY(String.class, ALLOW_CANCEL, ALLOW_UPDATE_RECIPIENTS),
        // Ungrouped stage with component message
        MIDDLE(Component.class, ALLOW_UPDATE_RECIPIENTS),
        // Grouped stage with component message
        LATE(Component.class, ALLOW_FORMAT_UPDATE),
        // Personal stage with component message
        POST(Component.class, ALLOW_FORMAT_UPDATE);

        private final Class<?> messageType;
        private final TransformRule[] rules;

        Stage(Class<?> messageType, TransformRule... rules) {
            this.messageType = messageType;
            this.rules = rules;
        }

        public boolean allows(TransformRule rule) {
            for (TransformRule transformRule : rules) {
                if (transformRule == rule) return true;
            }
            return false;
        }
    }

}
