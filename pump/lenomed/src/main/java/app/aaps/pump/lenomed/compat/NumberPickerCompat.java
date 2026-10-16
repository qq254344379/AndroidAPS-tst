package app.aaps.pump.lenomed.compat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import java.text.DecimalFormat;

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * The driver was reverse-engineered from a build that still had AAPS's own
 * {@code app.aaps.core.ui.elements.NumberPicker} - a {@link NumberPicker} subclass with a
 * {@code setParams(init, min, max, step, format, showButtons, button)} helper and a
 * {@code double getValue()}. That widget was removed from core:ui when the pump UIs moved to
 * Compose, so this shim reproduces the surface the driver actually uses, on top of the
 * platform {@link NumberPicker}.
 *
 * The platform picker only works on ints, so the decimal range is scaled by the step size:
 * a range of 0..25 with step 1.0 maps to min 0 / max 25, and a range of 0..5 with step 0.5
 * maps to min 0 / max 10. {@link #getValue()} scales back.
 */
public class NumberPickerCompat extends NumberPicker {

    private double step = 1.0d;
    private DecimalFormat formatter;

    public NumberPickerCompat(Context context) {
        super(context);
    }

    public NumberPickerCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPickerCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @param initValue    starting value
     * @param minValue     lower bound of the range
     * @param maxValue     upper bound of the range
     * @param stepSize     granularity, e.g. 0.5
     * @param fmt          formatter used for the displayed value
     * @param showButtons  kept for signature compatibility; the platform picker always shows its buttons
     * @param button       external button the driver wires up itself; only remembered here
     */
    public void setParams(double initValue, double minValue, double maxValue, double stepSize,
                          DecimalFormat fmt, boolean showButtons, View button) {
        this.step = stepSize > 0.0d ? stepSize : 1.0d;
        this.formatter = fmt;

        int minInt = (int) Math.round(minValue / this.step);
        int maxInt = (int) Math.round(maxValue / this.step);
        if (maxInt < minInt) {
            maxInt = minInt;
        }
        setMinValue(minInt);
        setMaxValue(maxInt);
        setWrapSelectorWheel(false);

        int initInt = (int) Math.round(initValue / this.step);
        if (initInt < minInt) {
            initInt = minInt;
        }
        if (initInt > maxInt) {
            initInt = maxInt;
        }
        setValue(initInt);
    }

    /** The currently selected value, scaled back from the picker's int range. */
    public double getDecimalValue() {
        return super.getValue() * step;
    }

    /** The currently selected value, formatted the way the driver asked for. */
    public String getValueFormatted() {
        double v = getValue();
        return formatter != null ? formatter.format(v) : String.valueOf(v);
    }
}
