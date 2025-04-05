package group.aelysium.rustyconnector.common.util;

import group.aelysium.rustyconnector.common.errors.Error;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public abstract class CommandClient {
    protected final Object source;
    
    public CommandClient(@NotNull Object source) {
        this.source = source;
    }
    
    public abstract void send(Component message);
    public abstract void send(Error error);
    public <S> S toSender() {
        return (S) this.source;
    }
    
    public static abstract class Player<P> extends CommandClient {
        public Player(@NotNull P source) {
            super(source);
        }
        
        public abstract String id();
        public abstract String username();
    }
    public static abstract class Console<C> extends CommandClient {
        public Console(@NotNull C source) {
            super(source);
        }
    }
    public static abstract class Other<O> extends CommandClient {
        public Other(@NotNull O source) {
            super(source);
        }
    }
}
