.class public final Lcom/xcam/offline/FujiDesignFeature;
.super Ljava/lang/Object;
.source "FujiDesignFeature.java"

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 4
    if-eqz p0, :done
    :try_start
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    const v1, -0xf0efee
    invoke-virtual {v0, v1}, Landroid/view/Window;->setStatusBarColor(I)V
    invoke-virtual {v0, v1}, Landroid/view/Window;->setNavigationBarColor(I)V
    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;
    move-result-object v2
    const/4 v3, 0x0
    invoke-virtual {v2, v3}, Landroid/view/View;->setSystemUiVisibility(I)V
    const v1, 0x01020002
    invoke-virtual {p0, v1}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v1
    if-eqz v1, :done
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiDesignFeature;->styleTree(Landroid/app/Activity;Landroid/view/View;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method private static styleTree(Landroid/app/Activity;Landroid/view/View;)V
    .locals 7
    if-eqz p1, :done
    instance-of v0, p1, Landroid/widget/Button;
    if-nez v0, :children
    instance-of v0, p1, Landroid/widget/TextView;
    if-eqz v0, :children
    check-cast p1, Landroid/widget/TextView;
    const v0, -0x111624
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setTextColor(I)V
    const-string v0, "sans-serif-condensed"
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroid/graphics/Typeface;->create(Ljava/lang/String;I)Landroid/graphics/Typeface;
    move-result-object v0
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V
    const v0, 0x3dcccccd
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setLetterSpacing(F)V
    :children
    instance-of v0, p1, Landroid/view/ViewGroup;
    if-eqz v0, :done
    check-cast p1, Landroid/view/ViewGroup;
    invoke-virtual {p1}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v0
    const/4 v1, 0x0
    :loop
    if-ge v1, v0, :done
    invoke-virtual {p1, v1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v2
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiDesignFeature;->styleTree(Landroid/app/Activity;Landroid/view/View;)V
    add-int/lit8 v1, v1, 0x1
    goto :loop
    :done
    return-void
.end method
