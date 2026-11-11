package app.aaps.core.interfaces.rx.events;

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * Used by the driver to request dismissal of a persistent notification by id.
 */
public class EventDismissNotification extends Event {

    private final int id;

    public EventDismissNotification(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
