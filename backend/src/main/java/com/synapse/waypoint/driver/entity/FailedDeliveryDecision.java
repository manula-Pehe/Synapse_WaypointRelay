package com.synapse.waypoint.driver.entity;

/**
 * What happens to a failed delivery. The store states a preference through with no answer by 2 PM the dispatcher re-plans
 * for tomorrow.
 */
public enum FailedDeliveryDecision {
    REPLAN_TOMORROW,
    TRY_LATER_TODAY,
    CANCEL
}