.class final Lcom/xcam/offline/ProCameraFeature$MenuClick;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;
.field private final activity:Landroid/app/Activity;
.method constructor <init>(Landroid/app/Activity;)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lcom/xcam/offline/ProCameraFeature$MenuClick;->activity:Landroid/app/Activity;
    return-void
.end method
.method public onClick(Landroid/content/DialogInterface;I)V
    .locals 1
    iget-object v0, p0, Lcom/xcam/offline/ProCameraFeature$MenuClick;->activity:Landroid/app/Activity;
    invoke-static {v0, p2}, Lcom/xcam/offline/ProCameraFeature;->showOptions(Landroid/app/Activity;I)V
    return-void
.end method