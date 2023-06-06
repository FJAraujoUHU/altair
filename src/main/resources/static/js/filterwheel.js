let fwConnected, filterName, filterPos, filterOffset, fwIsMoving;
const filterList = [];
$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    const source = new EventSource("/altair/api/filterwheel/stream");
    source.onmessage = function (event) {
        $("#fwConnect").prop('disabled', false);
        let data = JSON.parse(event.data);
        console.log(data);

        fwConnected = data.connected;
        filterName = data.curName;
        filterPos = parseInt(data.curPosition);
        filterOffset = parseInt(data.curOffset);
        fwIsMoving = data.isMoving;

        $("#fwChangeTo").prop('disabled', !(fwConnected && !fwIsMoving));
        $("#fwSelect").prop('disabled', !fwConnected);

        if (fwConnected) {
            $("#fwConnect").text("Disconnect");
            $("#fwSelect").removeClass("text-bg-primary-emphasis");
            $("#fwSelect").addClass("text-bg-primary");

            if (filterList.length === 0) populateFilterList();

        } else $("#fwConnect").text("Connect");

        if (fwIsMoving) {
            $("#fwCurrentPos").text("Moving...");

        } else {
            if (filterPos === -1) $("#fwCurrentPos").text("Unknown");
            $("#fwCurrentPos").text(filterPos);
        }
                
        $("#fwCurrentOff").text(filterOffset);
        $("#fwCurrentName").text(filterName);
    }

    // Set up controls
    $("#fwConnect").click(function () {
        let url = fwConnected ? "/altair/api/filterwheel/disconnect" : "/altair/api/filterwheel/connect";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#fwChangeTo").click(function () {
        let pos = $("#fwSelect").val();
        $.ajax({
            url: "/altair/api/filterwheel/setposition",
            type: "POST",
            data: {
                position: pos
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });
});

// Populate filter list
function populateFilterList() {
    $.ajax({
        url: "/altair/api/filterwheel/getfilternames",
        type: "GET",
        success: function (data) {
            filterList.length = 0;
            let select = $("#fwSelect");
            select.empty();
            $.each(data, function (i, name) {
                let option = "<option value='" + i + "'>" + name + "</option>";
                select.append(option);
                filterList.push(name);
            });
        },
        error: function (xhr, status, error) {
            console.log("Error: " + error);
        }
    });
}