$dirs = @(
    "assets/images",
    "assets/icons",
    "assets/fonts",
    "lib/core/constants",
    "lib/core/theme",
    "lib/core/utils",
    "lib/core/network",
    "lib/core/error",
    "lib/core/router",
    "lib/data/datasources/local",
    "lib/data/datasources/remote",
    "lib/data/models",
    "lib/data/repositories",
    "lib/domain/entities",
    "lib/domain/repositories",
    "lib/domain/usecases",
    "lib/presentation/providers",
    "lib/presentation/screens/splash",
    "lib/presentation/screens/auth",
    "lib/presentation/screens/dashboard",
    "lib/presentation/screens/rules",
    "lib/presentation/screens/channels",
    "lib/presentation/screens/reservations",
    "lib/presentation/screens/logs",
    "lib/presentation/screens/settings",
    "lib/presentation/screens/subscription",
    "lib/presentation/widgets/common",
    "lib/presentation/widgets/cards",
    "lib/presentation/widgets/forms",
    "lib/presentation/widgets/dialogs"
)

foreach ($dir in $dirs) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

Write-Host "Directory structure created successfully!"
