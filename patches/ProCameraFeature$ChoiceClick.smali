.class final Lcom/xcam/offline/ProCameraFeature$ChoiceClick;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;
.field private final activity:Landroid/app/Activity;
.field private final key:Ljava/lang/String;
.method constructor <init>(Landroid/app/Activity;Ljava/lang/String;)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;->activity:Landroid/app/Activity;
    iput-object p2, p0, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;->key:Ljava/lang/String;
    return-void
.end method
.method public onClick(Landroid/content/DialogInterface;I)V
    .locals 4
    :try_start
    iget-object v0, p0, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;->activity:Landroid/app/Activity;
    const-string v1, "pro_camera"
    const/4 v2, 0x0
    invoke-virtual {v0, v1, v2}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;
    move-result-object v0
    iget-object v1, p0, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;->key:Ljava/lang/String;
    invoke-interface {v0, v1, p2}, Landroid/content/SharedPreferences$Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences$Editor;->apply()V
    invoke-interface {p1}, Landroid/content/DialogInterface;->dismiss()V
    iget-object v0, p0, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;->activity:Landroid/app/Activity;
    invoke-static {v0}, Lcom/xcam/offline/ProCameraFeature;->rebind(Landroid/app/Activity;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    return-void
    :catch_all
    move-exception v0
    return-void
.end method