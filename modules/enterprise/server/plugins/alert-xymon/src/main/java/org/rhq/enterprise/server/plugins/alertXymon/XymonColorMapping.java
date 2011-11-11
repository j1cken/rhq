package org.rhq.enterprise.server.plugins.alertXymon;

/**
* Created by IntelliJ IDEA.
* User: torben
* Date: 1/19/11
* Time: 1:38 AM
* To change this template use File | Settings | File Templates.
*/
public enum XymonColorMapping {
    HIGH("red", 512),
    MEDIUM("yellow", 256),
    LOW("green", 128),
    UNDEF("clear", 64);

    private final String color;
    private final int prio;

    XymonColorMapping(String color, int prio) {
        this.color = color;
        this.prio = prio;
    }

    public String getColor() {
        return color;
    }

    public int getPrio() {
        return prio;
    }
}
