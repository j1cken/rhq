/**
 * file: util.js
 *
 * description: This script contains functions that provide core, low-level
 * functionality such as iterating over a collection or searching a collection.
 * This has no other script or library dependencies. To load the functions into
 * your CLI session execute the following from the CLI shell,
 *
 *   $ login <username> <password>
 *   $ exec -f samples/util.js
 *
 * Note that you must login before you can load the script.
 *
 * author: jsanda@redhat.com
 * author: lkrejci@redhat.com
 *
 * See also http://johnsanda.blogspot.com/search/label/javascript
 */


/**
 * If obj is a JS array or a java.util.Collection, each element is passed to
 * the callback function. If obj is a java.util.Map, each map entry is passed
 * to the callback function as a key/value pair. If obj is none of the
 * aforementioned types, it is treated as a generic object and each of its
 * properties is passed to the callback function as a name/value pair.
 */
function foreach(obj, fn) {
  if (obj instanceof Array) {
    for (i in obj) {
      fn(obj[i]);
    }
  }
  else if (obj instanceof java.util.Collection) {
    var iterator = obj.iterator();
    while (iterator.hasNext()) {
      fn(iterator.next());
    }
  }
  else if (obj instanceof java.util.Map) {
    var iterator = obj.entrySet().iterator()
    while (iterator.hasNext()) {
      var entry = iterator.next();
      fn(entry.key, entry.value);
    }
  }
  else {   // assume we have a generic object
    for (i in obj) {
      fn(i, obj[i]);
    }
  }
}

/**
 * Iterates over obj similar to foreach. fn should be a predicate that evaluates
 * to true or false. The first match that is found is returned.
 */
function find(obj, fn) {
  if (obj instanceof Array) {
    for (i in obj) {
      if (fn(obj[i])) {
        return obj[i]
      }
    }
  }
  else if (obj instanceof java.util.Collection) {
    var iterator = obj.iterator();
    while (iterator.hasNext()) {
      var next = iterator.next();
      if (fn(next)) {
        return next;
      }
    }
  }
  else if (obj instanceof java.util.Map) {
    var iterator = obj.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (fn(entry.key, entry.value)) {
        return {key: entry.key, value: entry.value};
      }
    }
  }
  else {
    for (i in obj) {
      if (fn(i, obj[i])) {
        return {key: i, value: obj[i]};
      }
    }
  }
  return null;
}

/**
 * Iterates over obj similar to foreach. fn should be a predicate that evaluates
 * to true or false. All of the matches are returned in a java.util.List.
 */
function findAll(obj, fn) {
  var matches = java.util.ArrayList();
  if ((obj instanceof Array) || (obj instanceof java.util.Collection)) {
    foreach(obj, function(element) {
      if (fn(element)) {
        matches.add(element);
      }
    });
  }
  else {
    foreach(obj, function(key, value) {
      if (fn(theKey, theValue)) {
        matches.add({key: theKey, value: theValue});
      }
    });
  }
  return matches;
}

/**
 * A convenience function to convert javascript hashes into RHQ's configuration
 * objects.
 * <p>
 * The conversion of individual keys in the hash follows these rules:
 * <ol>
 * <li> if a value of a key is a javascript array, it is interpreted as PropertyList
 * <li> if a value is a hash, it is interpreted as a PropertyMap
 * <li> otherwise it is interpreted as a PropertySimple
 * <li> a null or undefined value is ignored
 * </ol>
 * <p>
 * Note that the conversion isn't perfect, because the hash does not contain enough
 * information to restore the names of the list members.
 * <p>
 * Example: <br/>
 * <pre><code>
 * {
 *   simple : "value",
 *   list : [ "value1", "value2"],
 *   listOfMaps : [ { k1 : "value", k2 : "value" }, { k1 : "value2", k2 : "value2" } ]
 * }
 * </code></pre>
 * gets converted to a configuration object:
 * Configuration:
 * <ul>
 * <li> PropertySimple(name = "simple", value = "value")
 * <li> PropertyList(name = "list")
 *      <ol>
 *      <li>PropertySimple(name = "list", value = "value1")
 *      <li>PropertySimple(name = "list", value = "value2")
 *      </ol>
 * <li> PropertyList(name = "listOfMaps")
 *      <ol>
 *      <li> PropertyMap(name = "listOfMaps")
 *           <ul>
 *           <li>PropertySimple(name = "k1", value = "value")
 *           <li>PropertySimple(name = "k2", value = "value")
 *           </ul>
 *      <li> PropertyMap(name = "listOfMaps")
 *           <ul>
 *           <li>PropertySimple(name = "k1", value = "value2")
 *           <li>PropertySimple(name = "k2", value = "value2")
 *           </ul>
 *      </ol>
 * </ul>
 * Notice that the members of the list have the same name as the list itself
 * which generally is not the case.
 */
function asConfiguration(hash) {

	config = new Configuration;

	for(key in hash) {
		value = hash[key];

		if (value == null) {
			continue;
		}

		(function(parent, key, value) {
			function isArray(obj) {
				return typeof(obj) == 'object' && (obj instanceof Array);
			}

			function isHash(obj) {
				return typeof(obj) == 'object' && !(obj instanceof Array);
			}

			function isPrimitive(obj) {
				return typeof(obj) != 'object';
			}

			//this is an anonymous function, so the only way it can call itself
			//is by getting its reference via argument.callee. Let's just assign
			//a shorter name for it.
			var me = arguments.callee;

			var prop = null;

			if (isPrimitive(value)) {
				prop = new PropertySimple(key, new java.lang.String(value));
			} else if (isArray(value)) {
				prop = new PropertyList(key);
				for(var i = 0; i < value.length; ++i) {
					var v = value[i];
					if (v != null) {
						me(prop, key, v);
					}
				}
			} else if (isHash(value)) {
				prop = new PropertyMap(key);
				for(var i in value) {
					var v = value[i];
					if (value != null) {
						me(prop, i, v);
					}
				}
			}

			if (parent instanceof PropertyList) {
				parent.add(prop);
			} else {
				parent.put(prop);
			}
		})(config, key, value);
	}

	return config;
}

/**
 * Opposite of <code>asConfiguration</code>. Converts an RHQ's configuration object
 * into a javascript hash.
 *
 * @param configuration
 */
function asHash(configuration) {
	ret = {}

	iterator = configuration.getMap().values().iterator();
	while(iterator.hasNext()) {
		prop = iterator.next();

		(function(parent, prop) {
			function isArray(obj) {
				return typeof(obj) == 'object' && (obj instanceof Array);
			}

			function isHash(obj) {
				return typeof(obj) == 'object' && !(obj instanceof Array);
			}

			var me = arguments.callee;

			var representation = null;

			if (prop instanceof PropertySimple) {
				representation = prop.stringValue;
			} else if (prop instanceof PropertyList) {
				representation = [];

				for(var i = 0; i < prop.list.size(); ++i) {
					var child = prop.list.get(i);
					me(representation, child);
				}
			} else if (prop instanceof PropertyMap) {
				representation = {};

				var childIterator = prop.getMap().values().iterator();
				while(childIterator.hasNext()) {
					var child = childIterator.next();

					me(representation, child);
				}
			}

			if (isArray(parent)) {
				parent.push(representation);
			} else if (isHash(parent)) {
				parent[prop.name] = representation;
			}
		})(ret, prop);
	}
	(function(parent) {

	})(configuration);

	return ret;
}