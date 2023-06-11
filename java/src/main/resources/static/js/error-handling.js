function TokenError(cause) {
	this.cause = cause;
}

function RestError(status, body) {
	this.status = status;
	this.body = body;
}

function createErrorAlert(error) {
	console.log("error: " + error);

	$("#error-alert-body").empty();
	$("#error-alert").fadeOut();

	const alertContent = composeAlertContent(error);
	$("#error-alert-heading").html(alertContent.heading);

	const body = alertContent.body;
	if (body.type === "json") {
		$("#error-alert-body").append($("<pre>").text(body.text));
	} else {
		$("#error-alert-body").text(body.text);
	}

	$("#error-alert").addClass("show");
	$("#error-alert").fadeIn();
}

function composeAlertContent(error) {
	if (error instanceof RestError) {
		const responseBody = typeof error.body === "string" ? safeParse(error.body) : error.body;
		return {
			heading: `Server respond with error (status: ${error.status})`,
			body: {
				type: "json",
				text: JSON.stringify(responseBody, null, 2)
			}
		};
	} else if (error instanceof TokenError) {
		const cause = error.cause;
		return {
			heading:"Create token error",
			body: {
				type: "plain-text",
				text: `Could not create token because: error.type=${cause.type}, error.details=${cause.detail}`
			}
		};
	}
	return {
		heading: `Operation failed with error: ${error.name}`,
		body: {
			type: "plain-text",
			text: error.message
		}
	};
}

function safeParse(json) {
	try {
		return JSON.parse(json);
	} catch (error) {
		return json;
	}
}

$("#error-alert").on("close.bs.alert", function() {
	$("#error-alert").fadeOut();
	return false;
})
