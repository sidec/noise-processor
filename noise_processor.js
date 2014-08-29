goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.object', 'goog.string.StringBuffer', 'goog.array']);
goog.addDependency("../noise_processor/core.js", ['noise_processor.core'], ['cljs.core']);
goog.addDependency("../noise_processor/test.js", ['noise_processor.test'], ['cljs.core', 'noise_processor.core']);