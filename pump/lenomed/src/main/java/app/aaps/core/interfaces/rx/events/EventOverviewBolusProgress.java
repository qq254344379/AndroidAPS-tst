package app.aaps.core.interfaces.rx.events;

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * The original AAPS {@code EventOverviewBolusProgress} carried bolus-progress
 * information shown on the overview. The ported driver only sets {@code t},
 * {@code percent} and {@code status} and posts the event on the bus, so a
 * minimal shape is enough to compile.
 */
public class EventOverviewBolusProgress extends Event {

    private Treatment t;
    private int percent;
    private String status;

    public Treatment getT() {
        return t;
    }

    public void setT(Treatment t) {
        this.t = t;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static class Treatment {
        private double insulin;
        private final int carbs;
        private final boolean smb;
        private final Long id;

        public Treatment(double insulin, int carbs, boolean smb, Long id) {
            this.insulin = insulin;
            this.carbs = carbs;
            this.smb = smb;
            this.id = id;
        }

        public double getInsulin() {
            return insulin;
        }

        public void setInsulin(double insulin) {
            this.insulin = insulin;
        }

        public int getCarbs() {
            return carbs;
        }

        public boolean isSmb() {
            return smb;
        }

        public Long getId() {
            return id;
        }
    }
}
