var window = window ? window : {};
// Problems with resizing and jquery and chrome and this stuff is so dumb.
window.width = function() {
  return document.body.clientWidth;
};

window.height = function() {
  return document.body.clientHeight;
};

Object.values = function(o) {
  var values = [];
  Object.keys(o).forEach(function(k) { values.push(o[k]); });
  return values;
}