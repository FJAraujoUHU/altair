let state, isEnabled, isAdmin, isSafe, isSafeOverride, currentOrder, currentUser, remainingTime, nextNight, slaved, useAltairSlaving;

$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    const source = new EventSource("/altair/api/governor/stream");
    source.onmessage = function (event) {
        let data = JSON.parse(event.data);
        console.log(data);

        state = data.state;
        isEnabled = !state.toLowerCase().startsWith('disabled');
        isAdmin = state.toLowerCase().startsWith('admin');
        isSafe = data.isSafe;
        isSafeOverride = data.isSafeOverride;
        currentOrder = data.currentOrder;
        currentUser = data.currentUser;
        remainingTime = data.currentOrderRemainingTime;
        nextNight = data.nextNight;
        useAltairSlaving = (data.slaving.toLowerCase().endsWith('(altair)'));
        slaved = (data.slaving.toLowerCase().startsWith('slaved'));

        $("#gvState").text(state);
        $("#gvSafe").text(isSafe);
        $("#gvSafeOvr").text(isSafeOverride);
        $("#gvCurrOrder").text(data.currentOrder);
        $("#gvCurrUser").text(data.currentUser);
        $("#gvRemainingTime").text(data.currentOrderRemainingTime);
        $("#gvNextNight").text(data.nextNight);
        $("#gvSlaving").text(data.slaving);

        // Update the buttons
        if (isEnabled) {
            $("#gvEnable").text("Disable");
        } else {
            $("#gvEnable").text("Enable");
        }

        if (isAdmin) {
            $("#gvAdminMode").text("Exit Admin Mode");
        } else {
            $("#gvAdminMode").text("Enter Admin Mode");
        }

        if (slaved) {
            $("#gvSlavedTxt").text("Slaved");
        } else {
            $("#gvSlavedTxt").text("Not slaved");
        }
        $("#gvSlaved").prop('checked', slaved);

        if (useAltairSlaving) {
            $("#gvSlaveModeTxt").text("Altair Slaving");
        } else {
            $("#gvSlaveModeTxt").text("Native Slaving");
        }
        $("#gvSlaveMode").prop('checked', useAltairSlaving);

        // Disable controls if should be disabled
        $("#gvEnable").prop('disabled', isAdmin);
        $("#gvAdminMode").prop('disabled', false);
        $("#gvConnectAll").prop('disabled', false);
        $("#gvDisconnectAll").prop('disabled', false);
        $("#gvStart").prop('disabled', false);
        $("#gvStop").prop('disabled', false);
        $("#gvSlaved").prop('disabled', false);
        $("#gvSlaveMode").prop('disabled', false);        
    };

    // Set up controls
    $("#gvEnable").click(function () {
        let url = isEnabled ?  "/altair/api/governor/disable":"/altair/api/governor/enable";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvAdminMode").click(function () {
        let url = isAdmin ?  "/altair/api/governor/exitadminmode":"/altair/api/governor/enteradminmode";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvConnectAll").click(function () {
        $.ajax({
            url: "/altair/api/governor/connectall",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvDisconnectAll").click(function () {
        $.ajax({
            url: "/altair/api/governor/disconnectall",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvStart").click(function () {
        $.ajax({
            url: "/altair/api/governor/startobservatory",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvStop").click(function () {
        $.ajax({
            url: "/altair/api/governor/stopobservatory",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvSlaved").change(function () {
        let slaving = $(this).prop('checked');
        $.ajax({
            url: "/altair/api/governor/setslaving",
            type: "POST",
            data: {
                enable: slaving
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#gvSlaveMode").change(function () {
        let useAltair = $(this).prop('checked');
        $.ajax({
            url: "/altair/api/governor/usealtairslaving",
            type: "POST",
            data: {
                usealtair: useAltair
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });











});