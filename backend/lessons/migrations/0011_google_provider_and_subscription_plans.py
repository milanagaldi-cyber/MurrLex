import django.db.models.deletion
from django.db import migrations, models


def seed_plans(apps, schema_editor):
    Plan = apps.get_model("lessons", "SubscriptionPlan")
    Subscription = apps.get_model("lessons", "Subscription")
    plans = {}
    for code, name, period, credits in (
        ("light", "Light", "monthly", 250_000),
        ("super", "Super", "monthly", 1_000_000),
        ("ultra", "Ultra", "monthly", 4_000_000),
    ):
        plans[code], _ = Plan.objects.update_or_create(
            code=code,
            defaults={"name": name, "refill_period": period, "refill_credits": credits, "is_active": True},
        )
    for subscription in Subscription.objects.exclude(plan_code="free"):
        plan = plans.get(subscription.plan_code.lower())
        if plan:
            subscription.plan_id = plan.id
            subscription.save(update_fields=["plan"])


class Migration(migrations.Migration):
    dependencies = [("lessons", "0010_adminauditlog_creditledger_subscription_and_more")]
    operations = [
        migrations.CreateModel(
            name="SubscriptionPlan",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("code", models.SlugField(max_length=32, unique=True)),
                ("name", models.CharField(max_length=80)),
                ("refill_period", models.CharField(choices=[("daily", "Every day"), ("weekly", "Every week"), ("monthly", "Every month")], default="monthly", max_length=16)),
                ("refill_credits", models.PositiveBigIntegerField(default=0)),
                ("is_active", models.BooleanField(default=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
            ],
            options={"ordering": ["refill_credits", "name"]},
        ),
        migrations.AddField(model_name="subscription", name="credits_frozen", field=models.BooleanField(default=False)),
        migrations.AddField(model_name="subscription", name="freeze_reason", field=models.TextField(blank=True)),
        migrations.AddField(model_name="subscription", name="last_refilled_at", field=models.DateTimeField(blank=True, null=True)),
        migrations.AddField(model_name="subscription", name="next_refill_at", field=models.DateTimeField(blank=True, null=True)),
        migrations.AddField(model_name="subscription", name="plan", field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.PROTECT, related_name="subscriptions", to="lessons.subscriptionplan")),
        migrations.AlterField(model_name="creditledger", name="reason", field=models.CharField(choices=[("purchase", "Purchase"), ("usage", "Usage"), ("support_bonus", "Support bonus"), ("admin_adjustment", "Admin adjustment"), ("admin_deduction", "Admin deduction"), ("auto_refill", "Automatic subscription refill"), ("purchase_simulation", "Simulated purchase"), ("reversal", "Reversal")], max_length=32)),
        migrations.AlterField(model_name="providercredential", name="provider", field=models.CharField(choices=[("openai", "OpenAI"), ("elevenlabs", "ElevenLabs"), ("google", "Google AI Studio")], max_length=32, unique=True)),
        migrations.RunPython(seed_plans, migrations.RunPython.noop),
    ]
