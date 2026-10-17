/**
 * Master Dashboard - Firebase Cloud Functions
 * Modular Entry Point
 */

const auth = require("./src/auth");
const staff = require("./src/staff");
const orders = require("./src/orders");
const notifications = require("./src/notifications");
const subscriptions = require("./src/subscriptions");

module.exports = {
    ...auth,
    ...staff,
    ...orders,
    ...notifications,
    ...subscriptions
};
