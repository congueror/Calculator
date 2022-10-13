const MQ = MathQuill.getInterface(2);
const elements = [];

class StaticMath extends HTMLElement {
    connectedCallback() {
        elements.push(this);
    }
}
window.customElements.define('mth-f', StaticMath);