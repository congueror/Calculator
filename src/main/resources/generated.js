function generate(input) {
    if (document.getElementById("area") == null) {
        const area = document.createElement("div");
        area.width = window.innerWidth - 26;
        area.height = window.innerHeight / 2 - area.getBoundingClientRect().y
        area.id = "area"
        document.getElementById("generated").appendChild(area);
    }

    eval(input);

    for (let i = 0; i < elements.length; i++) {
        MQ.StaticMath(elements[i]);
    }
}

const axesColor = "rgb(255,128,128)";
const redraw = [];
let scroll = 0;

redraw.push(draw);

let cnv = document.getElementById("graph");
cnv.addEventListener('wheel', function (event) {
    event.preventDefault();
    if (event.deltaY > 0) {
        scroll--;
    } else {
        scroll++;
    }

    for (let i = 0; i < redraw.length; i++) {
        redraw[i].call();
    }
});

function fun(x) {
    return x - 1;
}

function draw() {
    var canvas = document.getElementById("graph");
    canvas.width = 750;
    canvas.height = 750;

    var axes = {}, ctx = canvas.getContext("2d");
    axes.x0 = .5 + .5 * canvas.width;  // x0 pixels from left to x=0
    axes.y0 = .5 + .5 * canvas.height; // y0 pixels from top to y=0
    axes.scale = 80 + (scroll > 0 ? (scroll % 8) * 3 : -(-scroll % 8) * 3);                   // pixels from x=0 to x=1 TODO: Make dynamic depending on function domain and range.
    axes.doNegativeX = true;

    showAxes(ctx, axes);
    funGraph(ctx, axes, fun, "rgb(66,44,255)", 1);
}

function funGraph(ctx, axes, func, color, thick) {
    var xx, yy, dx = 4, x0 = axes.x0, y0 = axes.y0, scale = 80 + scroll * 3;
    var iMax = Math.round((ctx.canvas.width - x0) / dx);
    var iMin = axes.doNegativeX ? Math.round(-x0 / dx) : 0;
    ctx.beginPath();
    ctx.lineWidth = thick;
    ctx.strokeStyle = color;

    for (var i = iMin; i <= iMax; i++) {
        xx = dx * i;
        yy = scale * func(xx / scale);
        if (i == iMin) ctx.moveTo(x0 + xx, y0 - yy);
        else ctx.lineTo(x0 + xx, y0 - yy);
    }
    ctx.stroke();
}

function showAxes(ctx, axes) {
    var x0 = axes.x0, w = ctx.canvas.width;
    var y0 = axes.y0, h = ctx.canvas.height;
    var xmin = axes.doNegativeX ? 0 : x0;

    drawGrid(ctx, axes);

    ctx.beginPath();
    ctx.strokeStyle = axesColor;
    ctx.moveTo(xmin, y0);
    ctx.lineTo(w, y0);  // X axis
    ctx.moveTo(x0, 0);
    ctx.lineTo(x0, h);  // Y axis
    ctx.stroke();

    for (let i = 10; i > 0; i--) {
        ctx.font = '20px mono-space';
        ctx.fillStyle = axesColor;
        let inx = -Math.floor(scroll / 8);
        let num;
        if (inx % 3 === 0) num = i * Math.pow(10, Math.floor(inx / 3));
        else if ((inx + 2) % 3 === 0) num = i * 2 * Math.pow(10, Math.floor((inx) / 3));
        else if ((inx - 2) % 3 === 0) num = i * 5 * Math.pow(10, Math.floor((inx) / 3));

        let num1 = -num;
        ctx.fillText((num).toString(), x0 + i * axes.scale - 5, y0 + 17);
        ctx.fillText((num1).toString(), x0 - i * axes.scale - 5, y0 + 17);
        ctx.fillText((num1).toString(), x0, y0 + i * axes.scale + 5);
        ctx.fillText((num).toString(), x0, y0 - i * axes.scale + 5);
    }

    ctx.closePath();
}

function drawGrid(ctx, axes) {
    var x0 = axes.x0;
    var y0 = axes.y0;

    ctx.beginPath();
    ctx.strokeStyle = "rgb(100, 100, 100)"
    ctx.globalAlpha = 1.0
    for (let i = 10; i > 0; i--) {
        ctx.moveTo(0, y0 - i * axes.scale);
        ctx.lineTo(ctx.canvas.width, y0 - i * axes.scale);
        ctx.moveTo(0, y0 + i * axes.scale);
        ctx.lineTo(ctx.canvas.width, y0 + i * axes.scale);

        ctx.moveTo(x0 - i * axes.scale, 0);
        ctx.lineTo(x0 - i * axes.scale, ctx.canvas.height);
        ctx.moveTo(x0 + i * axes.scale, 0);
        ctx.lineTo(x0 + i * axes.scale, ctx.canvas.height);
    }
    ctx.stroke();
    ctx.closePath();

    ctx.beginPath();
    ctx.strokeStyle = "rgb(100, 100, 100)"
    ctx.globalAlpha = 0.2
    for (let i = 50; i > 0; i--) {
        let s = axes.scale / 5;
        ctx.moveTo(0, y0 - i * s);
        ctx.lineTo(ctx.canvas.width, y0 - i * s);
        ctx.moveTo(0, y0 + i * s);
        ctx.lineTo(ctx.canvas.width, y0 + i * s);

        ctx.moveTo(x0 - i * s, 0);
        ctx.lineTo(x0 - i * s, ctx.canvas.height);
        ctx.moveTo(x0 + i * s, 0);
        ctx.lineTo(x0 + i * s, ctx.canvas.height);
    }
    ctx.stroke();
    ctx.closePath();

    ctx.globalAlpha = 1.0
}

function operationStep(area, math, message) {
    area.innerHTML += "<br>" +
        (message !== "" ? "<div id='text1'> " + message + "</div>" : "");

    let stepByStep = document.createElement("mth-f");
    stepByStep.id = "stepByStep";
    area.appendChild(stepByStep);
    stepByStep.innerHTML = math;

    area.innerHTML += "<br>";
}