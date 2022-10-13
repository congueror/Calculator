for (let i = 0; i < elements.length; i++) {
    MQ.StaticMath(elements[i]);
}

let eq = "";
let answerSpan = document.getElementById("input");
let answerMathField = MQ.MathField(answerSpan, {
    //spaceBehavesLikeTab: true;
    //leftRightIntoCmdGoes: false;
    handlers: {
        edit: function () {
            eq = answerMathField.latex(); // Get entered math in LaTeX format
        }
    }
});
answerMathField.focus();

draw();

function onAddButtonClick(latex) {
    answerMathField.write(latex);
    answerMathField.focus();
}

function onGoButtonClick() {
    if (eq !== "") {
        window.java({
            request: eq,
            persistent: false,
            onSuccess: function(response) {
                generate(response);
            },
            onFailure: function(error_code, error_message) {
                alert(error_code + ": " + error_message);
            }
        });
    }
}