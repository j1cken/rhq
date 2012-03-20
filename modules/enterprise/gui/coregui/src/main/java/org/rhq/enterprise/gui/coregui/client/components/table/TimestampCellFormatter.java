package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * Formats a timestamp (i.e. milliseconds since Epoch).
 *
 * @author Ian Springer
 */
public class TimestampCellFormatter implements CellFormatter {

    public static final DateTimeFormat DATE_TIME_FORMAT_FULL = DateTimeFormat
        .getFormat(PredefinedFormat.DATE_TIME_FULL);
    public static final DateTimeFormat DATE_TIME_FORMAT_LONG = DateTimeFormat
        .getFormat(PredefinedFormat.DATE_TIME_LONG);
    public static final DateTimeFormat DATE_TIME_FORMAT_MEDIUM = DateTimeFormat
        .getFormat(PredefinedFormat.DATE_TIME_MEDIUM);
    public static final DateTimeFormat DATE_TIME_FORMAT_SHORT = DateTimeFormat
        .getFormat(PredefinedFormat.DATE_TIME_SHORT);

    private DateTimeFormat dateTimeFormat;

    /**
     * Uses MEDIUM format.
     */
    public TimestampCellFormatter() {
        this(DATE_TIME_FORMAT_MEDIUM);
    }

    public TimestampCellFormatter(DateTimeFormat dateTimeFormat) {
        super();
        this.dateTimeFormat = dateTimeFormat;
    }

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        return format(value, dateTimeFormat);
    }

    /**
     * @param value
     * @return MEDIUM format for value
     */
    public static String format(Object value) {
        return format(value, DATE_TIME_FORMAT_MEDIUM);
    }

    /**
     * This is a public static so others can use the same date/time format as the cell formatter,
     * even if the caller doesn't have data from a cell or list grid. This can keep date formatting
     * consistent across the app, whether the data is in a cell or not.
     * 
     * @param value the date to format as a Date, Long, Integer or a String
     * @param dateTimeFormat the format to use. If null defaults to SHORT format
     *
     * @return the formatted date string
     */
    public static String format(Object value, DateTimeFormat dateTimeFormat) {
        if (value == null) {
            return "";
        }
        Date date;
        if (value instanceof Date) {
            date = (Date) value;
        } else {
            long longValue;
            if (value instanceof Long) {
                longValue = (Long) value;
            } else if (value instanceof Integer) {
                longValue = (Integer) value;
            } else if (value instanceof String) {
                try {
                    longValue = Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    // allow special string values
                    return (String) value;
                }
            } else {
                throw new IllegalArgumentException("value parameter is not a Date, Long, Integer, or a String.");
            }
            if (longValue == 0) {
                return "";
            }
            date = new Date(longValue);
        }

        return (null == dateTimeFormat) ? DATE_TIME_FORMAT_MEDIUM.format(date) : dateTimeFormat.format(date);
    }

    public static void prepareDateField(final ListGridField field) {
        prepareDateField(field, field.getName());
    }

    public static void prepareDateField(final ListGridField field, final String dateTimeAttributeName) {
        field.setCellFormatter(new TimestampCellFormatter());
        field.setShowHover(true);
        field.setHoverCustomizer(getHoverCustomizer(dateTimeAttributeName));
    }

    public static HoverCustomizer getHoverCustomizer(final String dateTimeAttributeName) {
        return new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value instanceof Date) {
                    Date attribValue = record.getAttributeAsDate(dateTimeAttributeName);
                    return getHoverDateString(attribValue);
                }
                return value.toString();
            }
        };
    }

    /**
     * Returns an HTML string that can be used in as hover for a date field.
     * It formats the date in the FULL format.
     * 
     * @param date
     * @return HTML string that shows the date in FULL format,
     *         or <code>null</code> if <code>date</code> is <code>null</code>
     */
    public static String getHoverDateString(Date date) {
        if (date != null) {
            StringBuilder sb = new StringBuilder("<p style='width:300px'>");
            sb.append(format(date, DATE_TIME_FORMAT_FULL));
            sb.append("</p>");
            return sb.toString();
        } else {
            return null;
        }
    }
}
