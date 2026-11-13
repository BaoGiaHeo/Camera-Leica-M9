.class public final Lcom/xcam/offline/FujiDesignFeature;
.super Ljava/lang/Object;
.source "FujiDesignFeature.java"

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 3
    if-eqz p0, :done
    :try_start
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    const v1, -0xf0efee
    invoke-virtual {v0, v1}, Landroid/view/Window;->setStatusBarColor(I)V
    invoke-virtual {v0, v1}, Landroid/view/Window;->setNavigationBarColor(I)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v2
    return-void
.end method
