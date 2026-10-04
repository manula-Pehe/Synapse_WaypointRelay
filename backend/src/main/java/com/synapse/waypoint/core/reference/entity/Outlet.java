package com.synapse.waypoint.core.reference.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A store that receives deliveries (table {@code outlets}, V1). Window times are local wall-clock
 * times; the mall window applies instead of the normal one when present.
 */
@Entity
@Table(name = "outlets")
public class Outlet {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 10)
    private String brand;

    @Column(nullable = false, length = 40)
    private String district;

    @Column(nullable = false, length = 20)
    private String depot;

    @Column(name = "dock_type", nullable = false, length = 20)
    private String dockType;

    @Column(name = "parking_constraint", nullable = false, length = 20)
    private String parkingConstraint;

    @Column(name = "mall_window_open")
    private LocalTime mallWindowOpen;

    @Column(name = "mall_window_close")
    private LocalTime mallWindowClose;

    @Column(name = "window_open", nullable = false)
    private LocalTime windowOpen;

    @Column(name = "window_close", nullable = false)
    private LocalTime windowClose;

    protected Outlet() {
        // for JPA
    }

    public Outlet(String id, String brand, String district, String depot, String dockType, String parkingConstraint,
            LocalTime mallWindowOpen, LocalTime mallWindowClose, LocalTime windowOpen, LocalTime windowClose) {
        this.id = id;
        this.brand = brand;
        this.district = district;
        this.depot = depot;
        this.dockType = dockType;
        this.parkingConstraint = parkingConstraint;
        this.mallWindowOpen = mallWindowOpen;
        this.mallWindowClose = mallWindowClose;
        this.windowOpen = windowOpen;
        this.windowClose = windowClose;
    }

    public String getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getDistrict() {
        return district;
    }

    public String getDepot() {
        return depot;
    }

    public String getDockType() {
        return dockType;
    }

    public String getParkingConstraint() {
        return parkingConstraint;
    }

    public LocalTime getMallWindowOpen() {
        return mallWindowOpen;
    }

    public LocalTime getMallWindowClose() {
        return mallWindowClose;
    }

    public LocalTime getWindowOpen() {
        return windowOpen;
    }

    public LocalTime getWindowClose() {
        return windowClose;
    }
}
