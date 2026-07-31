package com.oxohang.fanfreeform.xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.app.Application;
import android.app.Instrumentation;
import android.view.MotionEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class FanFreeformHook implements IXposedHookLoadPackage {
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String MIUI_HOME = "com.miui.home";
    private static final String EVENT_CONTROLLER =
            "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MulWinSwitchEventController";
    private static final String EVENT_HANDLER = EVENT_CONTROLLER + "$EventHandler";
    private static final String FREEFORM_CONTROLLER =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeController";
    private static final String FREEFORM_PIN_HANDLER =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModePinHandler";
    private static final String FREEFORM_ANIMATION =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeAnimation";

    @SuppressLint("StaticFieldLeak")
    private static volatile FanRuntime runtime;
    private static volatile Object freeformController;
    private static volatile Object eventProxy;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (MIUI_HOME.equals(loadPackageParam.packageName)) {
            hookShortcutHost(loadPackageParam.classLoader);
            hookRecentsClearButton(loadPackageParam.classLoader);
            return;
        }
        if (!SYSTEM_UI.equals(loadPackageParam.packageName)) return;
        Log.i("Loading in SystemUI process=" + loadPackageParam.processName);
        hookFreeformController(loadPackageParam.classLoader);
        hookFreeformPinHandler(loadPackageParam.classLoader);
        hookMiniAnimationTarget(loadPackageParam.classLoader);
        hookEdgePinRestoreTarget(loadPackageParam.classLoader);
        hookShortcutEnterAnimation(loadPackageParam.classLoader);
        hookLandscapeWindowShape(loadPackageParam.classLoader);
        hookInputController(loadPackageParam.classLoader);
        hookStatusBarTouchEntry(loadPackageParam.classLoader);
        hookShadeExpansion(loadPackageParam.classLoader);
        hookControlCenterExpansion(loadPackageParam.classLoader);
        hookAuthoritativePanelState(loadPackageParam.classLoader);
    }

    private static void hookRecentsClearButton(ClassLoader classLoader) {
        try {
            Class<?> container = XposedHelpers.findClassIfExists(
                    "com.miui.home.recents.views.RecentsContainer", classLoader);
            if (container == null) {
                Log.i("HyperOS recents clear-button hook skipped: container class missing");
                return;
            }
            XC_MethodHook updater = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    RecentsClearButtonRuntime.attach(param.thisObject);
                }
            };
            XposedBridge.hookAllMethods(container, "onFinishInflate", updater);
            XposedBridge.hookAllMethods(container, "onAttachedToWindow", updater);
            Log.i("HyperOS recents clear-button hook installed");
        } catch (Throwable error) {
            Log.e("HyperOS recents clear-button hook failed safely", error);
        }
    }

    private static void hookStatusBarTouchEntry(ClassLoader classLoader) {
        hookSystemPanelTouchView(classLoader,
                "com.android.systemui.statusbar.window.StatusBarWindowView",
                "status bar");
        hookSystemPanelTouchView(classLoader,
                "com.android.systemui.shade.NotificationShadeWindowView",
                "notification shade window");
    }

    private static void hookSystemPanelTouchView(ClassLoader classLoader,
                                                 String className, String source) {
        try {
            Class<?> statusBarWindow = XposedHelpers.findClassIfExists(
                    className, classLoader);
            if (statusBarWindow == null) return;
            XposedBridge.hookAllMethods(statusBarWindow, "dispatchTouchEvent",
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            if (runtime == null || param.args.length == 0
                                    || !(param.args[0] instanceof MotionEvent)) return;
                            MotionEvent event = (MotionEvent) param.args[0];
                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                runtime.onSystemPanelTouchStarted(source);
                            }
                        }
                    });
            Log.i(source + " touch-entry hook installed");
        } catch (Throwable error) {
            Log.e(source + " touch-entry hook failed safely", error);
        }
    }

    private static void hookShortcutHost(ClassLoader classLoader) {
        XC_MethodHook installer = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.args.length == 0 || !(param.args[0] instanceof Context)) return;
                ShortcutHostRuntime.install((Context) param.args[0]);
            }
        };
        XposedBridge.hookAllMethods(Application.class, "attach", installer);
        XposedBridge.hookAllMethods(Instrumentation.class, "callApplicationOnCreate",
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args.length == 0 || !(param.args[0] instanceof Application)) return;
                        ShortcutHostRuntime.install((Application) param.args[0]);
                    }
                });
        Log.i("Shortcut host hook installed in MiuiHome");
    }

    private static void hookShadeExpansion(ClassLoader classLoader) {
        try {
            Class<?> manager = XposedHelpers.findClassIfExists(
                    "com.android.systemui.shade.ShadeExpansionStateManager", classLoader);
            if (manager == null) return;
            XposedBridge.hookAllMethods(manager, "onPanelExpansionChanged", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime == null || param.args.length < 2
                            || !(param.args[0] instanceof Number)
                            || !(param.args[1] instanceof Boolean)) return;
                    runtime.onShadeExpansionChanged(((Number) param.args[0]).floatValue(),
                            (Boolean) param.args[1]);
                }
            });
            Log.i("Notification shade expansion hook installed");
        } catch (Throwable error) {
            Log.e("Notification shade hook failed safely", error);
        }
    }

    private static void hookControlCenterExpansion(ClassLoader classLoader) {
        try {
            Class<?> eventHandler = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controlcenter.container.ControlCenterEventHandlerImpl",
                    classLoader);
            if (eventHandler != null) {
                XposedBridge.hookAllMethods(eventHandler, "handleExpandEvent",
                        new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                if (runtime == null || param.args.length == 0
                                        || !(param.args[0] instanceof MotionEvent)) return;
                                MotionEvent event = (MotionEvent) param.args[0];
                                if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                                        && Boolean.TRUE.equals(param.getResult())) {
                                    runtime.onControlCenterExpansionChanged(true);
                                }
                            }
                        });
                Log.i("Control center touch-entry hook installed");
            }
            Class<?> controlCenter = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controlcenter.ControlCenterImpl", classLoader);
            if (controlCenter != null) {
                XposedBridge.hookAllMethods(controlCenter, "expand", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(true);
                    }
                });
                XposedBridge.hookAllMethods(controlCenter, "switchShow", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(true);
                    }
                });
                XposedBridge.hookAllMethods(controlCenter, "collapse", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(false);
                    }
                });
                XposedBridge.hookAllMethods(controlCenter, "switchHide", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(false);
                    }
                });
            }
            Class<?> panelInjector = XposedHelpers.findClassIfExists(
                    "com.android.systemui.shade.NotificationPanelViewControllerInjector", classLoader);
            if (panelInjector != null) {
                XposedBridge.hookAllMethods(panelInjector, "onControlCenterAppearChanged",
                        new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                if (runtime == null || param.args.length == 0
                                        || !(param.args[0] instanceof Boolean)) return;
                                runtime.onControlCenterExpansionChanged((Boolean) param.args[0]);
                            }
                        });
            }
            Class<?> headerController = XposedHelpers.findClassIfExists(
                    "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController",
                    classLoader);
            if (headerController != null) {
                XposedBridge.hookAllMethods(headerController, "onExpansionChanged",
                        new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                if (runtime == null || param.args.length == 0
                                        || !(param.args[0] instanceof Number)) return;
                                // This controller can continue reporting 0 while its panel is
                                // visible, so only treat a positive progress as an enter signal.
                                if (((Number) param.args[0]).floatValue() > 0.01f) {
                                    runtime.onControlCenterExpansionChanged(true);
                                }
                            }
                        });
            }
            Log.i("Control center expansion hooks installed");
        } catch (Throwable error) {
            Log.e("Control center expansion hooks failed safely", error);
        }
    }

    private static void hookAuthoritativePanelState(ClassLoader classLoader) {
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                FanRuntime active = runtime;
                if (active == null) return;
                try {
                    Object observer = XposedHelpers.getObjectField(param.thisObject, "this$0");
                    boolean notification = XposedHelpers.getBooleanField(
                            observer, "mNotificationPanelExpand");
                    boolean control = XposedHelpers.getBooleanField(
                            observer, "mControlCenterExpand");
                    active.onAuthoritativeSystemPanelState(notification, control);
                } catch (Throwable error) {
                    Log.e("Cannot read authoritative system panel state", error);
                }
            }
        };
        try {
            Class<?> controlListener = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controller.GestureObserver$3", classLoader);
            Class<?> notificationListener = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controller.GestureObserver$4", classLoader);
            if (controlListener != null) {
                XposedBridge.hookAllMethods(controlListener, "onExpandChanged", callback);
            }
            if (notificationListener != null) {
                XposedBridge.hookAllMethods(notificationListener, "onPanelExpanded", callback);
            }
            Class<?> stateCommit = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controller.GestureObserver$$ExternalSyntheticLambda2",
                    classLoader);
            if (stateCommit != null) {
                XposedBridge.hookAllMethods(stateCommit, "run", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        FanRuntime active = runtime;
                        if (active == null) return;
                        try {
                            boolean control = XposedHelpers.getBooleanField(
                                    param.thisObject, "f$1");
                            boolean notification = XposedHelpers.getBooleanField(
                                    param.thisObject, "f$2");
                            active.onAuthoritativeSystemPanelState(notification, control);
                        } catch (Throwable error) {
                            Log.e("Cannot read committed system panel state", error);
                        }
                    }
                });
            }
            Log.i("Authoritative system panel state hooks installed");
        } catch (Throwable error) {
            Log.e("Authoritative system panel state hooks failed safely", error);
        }
    }

    private static void hookInputController(ClassLoader classLoader) {
        try {
            Class<?> controllerClass = XposedHelpers.findClassIfExists(EVENT_CONTROLLER, classLoader);
            Class<?> handlerClass = XposedHelpers.findClassIfExists(EVENT_HANDLER, classLoader);
            if (controllerClass == null || handlerClass == null) {
                Log.i("HyperOS input controller is unavailable; module disabled safely");
                return;
            }
            XposedBridge.hookAllMethods(controllerClass, "createEventReceiver", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length == 0 || !(param.args[0] instanceof Context)) return;
                    installRuntime((Context) param.args[0], classLoader, param.thisObject, handlerClass);
                }
            });
            Log.i("Input controller hook installed");
        } catch (Throwable error) {
            Log.e("Input controller hook failed safely", error);
        }
    }

    private static synchronized void installRuntime(Context context, ClassLoader classLoader,
                                                    Object inputController, Class<?> handlerClass) {
        try {
            if (runtime == null) {
                runtime = new FanRuntime(context, classLoader);
                if (freeformController != null) runtime.setFreeformController(freeformController);
            }
            if (eventProxy == null) {
                eventProxy = Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class<?>[]{handlerClass},
                        (proxy, method, args) -> dispatchProxy(proxy, method, args));
                XposedHelpers.callMethod(inputController, "registerEventHandler", eventProxy);
                Log.i("Fan gesture event handler registered");
            }
            runtime.reportInputReady();
        } catch (Throwable error) {
            Log.e("Runtime installation failed safely", error);
        }
    }

    private static Object dispatchProxy(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("onEvent".equals(name) && args != null && args.length >= 2 && args[0] instanceof MotionEvent) {
            FanRuntime active = runtime;
            if (active != null) active.onMotion((MotionEvent) args[0], args[1]);
            return null;
        }
        if ("toString".equals(name)) return "FanFreeformEventHandler";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return args != null && args.length == 1 && proxy == args[0];
        return null;
    }

    private static void hookFreeformController(ClassLoader classLoader) {
        try {
            Class<?> controllerClass = XposedHelpers.findClassIfExists(FREEFORM_CONTROLLER, classLoader);
            if (controllerClass == null) {
                Log.i("HyperOS freeform controller is unavailable; launch tracking disabled safely");
                return;
            }
            XposedBridge.hookAllConstructors(controllerClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    freeformController = param.thisObject;
                    FanRuntime active = runtime;
                    if (active != null) active.setFreeformController(param.thisObject);
                }
            });
            XposedBridge.hookAllMethods(controllerClass, "onTaskAppeared", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime != null && param.args.length > 0 && param.args[0] instanceof Number) {
                        runtime.onTaskAppeared(((Number) param.args[0]).intValue());
                    }
                }
            });
            XposedBridge.hookAllMethods(controllerClass, "onTaskModeChanged", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime != null && param.args.length > 0) runtime.onTaskInfo(param.args[0]);
                }
            });
            XposedBridge.hookAllMethods(controllerClass, "onTaskVanished", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime != null && param.args.length > 0 && param.args[0] instanceof Number) {
                        runtime.onTaskVanished(((Number) param.args[0]).intValue());
                    }
                }
            });
            XC_MethodHook leavingInteractiveStateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    FanRuntime active = runtime;
                    if (active == null) return;
                    active.onNativeFreeformLeavingInteractiveState(param.method.getName());
                }
            };
            XposedBridge.hookAllMethods(controllerClass, "startPinAnimation",
                    leavingInteractiveStateHook);
            XposedBridge.hookAllMethods(controllerClass, "fromFreeformToMini",
                    leavingInteractiveStateHook);
            XposedBridge.hookAllMethods(controllerClass, "onImeVisibilityChanged",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (runtime == null || param.args.length < 2
                                    || !(param.args[0] instanceof Boolean)
                                    || !(param.args[1] instanceof Number)) return;
                            runtime.onImeVisibilityChanged((Boolean) param.args[0],
                                    ((Number) param.args[1]).intValue());
                        }
                    });
            XposedBridge.hookAllMethods(controllerClass, "handleTouchEvent",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            FanRuntime active = runtime;
                            if (active == null || param.args.length == 0
                                    || !(param.args[0] instanceof MotionEvent)) return;
                            active.onNativeFreeformMotion((MotionEvent) param.args[0]);
                        }
                    });
            Log.i("Freeform controller hooks installed");
        } catch (Throwable error) {
            Log.e("Freeform controller hooks failed safely", error);
        }
    }

    private static void hookFreeformPinHandler(ClassLoader classLoader) {
        try {
            Class<?> handlerClass = XposedHelpers.findClassIfExists(
                    FREEFORM_PIN_HANDLER, classLoader);
            if (handlerClass == null) {
                Log.i("HyperOS freeform pin handler is unavailable; direct pin tracking disabled");
                return;
            }
            XposedBridge.hookAllMethods(handlerClass, "startPinAnimation",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            FanRuntime active = runtime;
                            if (active != null) {
                                active.onNativeFreeformLeavingInteractiveState(
                                        "pin handler startPinAnimation");
                            }
                        }
                    });
            Log.i("Freeform pin handler hook installed");
        } catch (Throwable error) {
            Log.e("Freeform pin handler hook failed safely", error);
        }
    }

    private static void hookMiniAnimationTarget(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass == null) {
                Log.i("HyperOS freeform animation unavailable; mini target correction disabled");
                return;
            }
            XposedBridge.hookAllMethods(animationClass, "startGestureAnimation",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            FanRuntime active = runtime;
                            if (active == null || param.args.length < 3
                                    || !(param.args[0] instanceof Number)) return;
                            active.adjustMiniTargetIfNeeded(
                                    ((Number) param.args[0]).intValue(),
                                    param.args[1], param.args[2]);
                        }
                    });
            Log.i("Freeform mini animation target hook installed");
        } catch (Throwable error) {
            Log.e("Freeform mini animation target hook failed safely", error);
        }
    }

    private static void hookEdgePinRestoreTarget(ClassLoader classLoader) {
        try {
            Class<?> pinHandlerClass = XposedHelpers.findClassIfExists(
                    FREEFORM_PIN_HANDLER, classLoader);
            if (pinHandlerClass != null) {
                XposedBridge.hookAllMethods(pinHandlerClass, "setUnPinAnimInfo",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                FanRuntime active = runtime;
                                if (active == null || param.args.length < 5
                                        || !(param.args[1] instanceof Number)) return;
                                int animationType = ((Number) param.args[1]).intValue();
                                if (animationType == 8 || animationType == 9) {
                                    active.adjustEdgePinRestoreTarget(
                                            param.args[0], param.args[4], null);
                                }
                            }
                        });
            }

            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass != null) {
                XposedBridge.hookAllMethods(animationClass, "startUnpinShellTransition",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                FanRuntime active = runtime;
                                if (active == null || param.args.length < 3) return;
                                active.adjustEdgePinRestoreTarget(
                                        param.args[0], null, param.args[2]);
                            }
                        });
            }
            Log.i("Freeform edge-pin restore target hooks installed");
        } catch (Throwable error) {
            Log.e("Freeform edge-pin restore target hooks failed safely", error);
        }
    }

    private static void hookShortcutEnterAnimation(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass == null) return;
            XC_MethodHook hook = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    FanRuntime active = runtime;
                    if (active == null) return;
                    Object change = null;
                    Object taskInfo = null;
                    for (Object argument : param.args) {
                        if (argument == null) continue;
                        String name = argument.getClass().getName();
                        if (name.endsWith("TransitionInfo$Change")) {
                            change = argument;
                        } else if (name.endsWith("MiuiFreeformModeTaskInfo")) {
                            taskInfo = argument;
                        }
                    }
                    if (change != null && taskInfo != null) {
                        active.prepareShortcutEnterAnimation(change, taskInfo);
                    }
                }
            };
            XposedBridge.hookAllMethods(animationClass,
                    "startMoveToFrontAnimation", hook);
            XposedBridge.hookAllMethods(animationClass,
                    "startFullScreenToFreeformAnimation", hook);
            Log.i("Shortcut enter animation alignment hooks installed");
        } catch (Throwable error) {
            Log.e("Shortcut enter animation alignment hooks failed safely", error);
        }
    }

    private static void hookLandscapeWindowShape(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            int orientationHookCount = 0;
            if (animationClass != null) {
                XC_MethodHook orientationHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        FanRuntime active = runtime;
                        if (active == null || param.args.length == 0) return;
                        if (active.stabilizeLandscapeWindowShape(param.args[0])) {
                            param.setResult(null);
                        }
                    }
                };
                orientationHookCount += XposedBridge.hookAllMethods(animationClass,
                        "adjustFreeformOrientationIfNeed", orientationHook).size();
                orientationHookCount += XposedBridge.hookAllMethods(animationClass,
                        "lambda$startFreeformOrientationChangeShellTransition$4",
                        orientationHook).size();
            }
            int requestHookCount = 0;
            Class<?> controllerClass = XposedHelpers.findClassIfExists(
                    FREEFORM_CONTROLLER, classLoader);
            if (controllerClass != null) {
                requestHookCount = XposedBridge.hookAllMethods(controllerClass,
                        "setRequestedOrientation", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                FanRuntime active = runtime;
                                if (active == null || param.args.length < 2
                                        || !(param.args[0] instanceof Number)
                                        || !(param.args[1] instanceof Number)) return;
                                int taskId = ((Number) param.args[0]).intValue();
                                int orientation = ((Number) param.args[1]).intValue();
                                if (active.suppressLandscapeShapeChange(taskId, orientation)) {
                                    param.setResult(null);
                                }
                            }
                        }).size();
            }
            Log.i("Configured landscape freeform shape hooks installed orientation="
                    + orientationHookCount + " requested=" + requestHookCount);
        } catch (Throwable error) {
            Log.e("Configured landscape freeform shape hooks failed safely", error);
        }
    }

}
