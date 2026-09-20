package com.sunflower.shortcut.automation.runtime

/**
 * JS injected into the WebView before the user's scenario code runs. It
 * gives the scenario plain, standard JavaScript (spec §15: "JS должен
 * оставаться обычным JavaScript... не создавать отдельный язык
 * программирования") — on(), wait(), and one proxy object per sdk.* module
 * name. Every module call is forwarded generically through the native
 * bridge; the *methods* on each module (camera.takePhoto, apps.launch, ...)
 * are validated and implemented natively by the sdk/ package, not here —
 * this file only needs to know the module *names* from spec §15/§16/§17.
 */
object JsHostScript {

    val MODULE_NAMES = listOf(
        "android", "apps", "ui", "camera", "location", "files", "network",
        "notifications", "audio", "bluetooth", "wifi", "sensors", "media",
        "contacts", "phone", "clipboard", "storage", "system", "automation"
    )

    val BOOTSTRAP: String = buildString {
        append(
            """
            (function () {
                const __handlers = {};
                let __callId = 0;
                const __pending = {};

                window.on = function (event, handler) {
                    __handlers[event] = handler;
                };

                window.wait = function (ms) {
                    return new Promise(function (resolve) { setTimeout(resolve, ms); });
                };

                window.__invoke = function (event, payloadJson) {
                    const handler = __handlers[event];
                    if (!handler) {
                        return Promise.reject(new Error('Нет обработчика для события "' + event + '"'));
                    }
                    let payload = {};
                    try { payload = JSON.parse(payloadJson); } catch (e) {}
                    return Promise.resolve(handler(payload));
                };

                window.__resolveNative = function (id, resultJson) {
                    const pending = __pending[id];
                    if (!pending) return;
                    delete __pending[id];
                    let parsed;
                    try { parsed = JSON.parse(resultJson); } catch (e) {
                        pending.reject(new Error('Некорректный ответ от нативного модуля'));
                        return;
                    }
                    if (parsed && parsed.error) {
                        pending.reject(new Error(parsed.error));
                    } else {
                        pending.resolve(parsed ? parsed.value : undefined);
                    }
                };

                window.__nativeCall = function (moduleName, method, args) {
                    return new Promise(function (resolve, reject) {
                        const id = String(__callId++);
                        __pending[id] = { resolve: resolve, reject: reject };
                        NativeBridge.invoke(id, moduleName, method, JSON.stringify(args || []));
                    });
                };

                function makeModule(name) {
                    return new Proxy({}, {
                        get: function (_target, method) {
                            if (typeof method !== 'string') return undefined;
                            return function () {
                                const args = Array.prototype.slice.call(arguments);
                                return window.__nativeCall(name, method, args);
                            };
                        }
                    });
                }

                // ui.* is special-cased rather than a flat module proxy: find*()
                // returns a plain object synchronously (no bridge round-trip yet)
                // so `ui.findText("X").click()` — spec §17's exact syntax — works.
                // The actual find-and-act happens as ONE native call when a
                // terminal method (click/exists/readText) is invoked, passing the
                // locator strategy + value together — no server-side handle or
                // lifecycle to manage.
                function makeUiLocator(strategy, value) {
                    return {
                        click: function () { return window.__nativeCall('ui', 'click', [strategy, value]); },
                        exists: function () { return window.__nativeCall('ui', 'exists', [strategy, value]); },
                        readText: function () { return window.__nativeCall('ui', 'readText', [strategy, value]); }
                    };
                }

                window.ui = {
                    findText: function (text) { return makeUiLocator('text', text); },
                    findId: function (id) { return makeUiLocator('id', id); },
                    findDescription: function (description) { return makeUiLocator('description', description); },
                    waitForText: function (text, timeoutMs) { return window.__nativeCall('ui', 'waitForText', [text, timeoutMs]); },
                    type: function (text) { return window.__nativeCall('ui', 'type', [text]); },
                    scroll: function (direction) { return window.__nativeCall('ui', 'scroll', [direction]); },
                    back: function () { return window.__nativeCall('ui', 'back', []); },
                    home: function () { return window.__nativeCall('ui', 'home', []); }
                };

            """.trimIndent()
        )
        MODULE_NAMES.filter { it != "ui" }.forEach { name ->
            append("                window.$name = makeModule('$name');\n")
        }
        append(
            """
            })();
            """.trimIndent()
        )
    }
}
