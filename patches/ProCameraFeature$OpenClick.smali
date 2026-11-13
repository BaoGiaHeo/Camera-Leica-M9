.class final Lcom/xcam/offline/ProCameraFeature$OpenClick;
.super Ljava/lang/Object;
.implements Landroid/view/View$OnClickListener;
.field private final activity:Landroid/app/Activity;
.method constructor <init>(Landroid/app/Activity;)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lcom/xcam/offline/ProCameraFeature$OpenClick;->activity:Landroid/app/Activity;
    return-void
.end method
.method public onClick(Landroid/view/View;)V
    .locals 1
    iget-object v0, p0, Lcom/xcam/offline/ProCameraFeature$OpenClick;->activity:Landroid/app/Activity;
    invoke-static {v0}, Lcom/xcam/offline/ProCameraFeature;->show(Landroid/app/Activity;)V
    return-void
.end method