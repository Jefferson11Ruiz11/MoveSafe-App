package com.app.riesgo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.riesgo.data.model.CameraBiomechanicalMetrics
import com.app.riesgo.data.model.PhysicalTest
import com.app.riesgo.data.model.SportType
import com.app.riesgo.data.model.ZoneRiskDto
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.data.repository.EvaluationRepository
import com.app.riesgo.data.repository.InjuryRiskEngine
import com.app.riesgo.ui.screens.assessment.CameraExerciseScreen
import com.app.riesgo.ui.screens.assessment.FormScreen
import com.app.riesgo.ui.screens.assessment.InjurySurveyScreen
import com.app.riesgo.ui.screens.assessment.PostTestFeedbackScreen
import com.app.riesgo.ui.screens.assessment.ResultsScreen
import com.app.riesgo.ui.screens.assessment.SelectionScreen
import com.app.riesgo.ui.screens.auth.LoginScreen
import com.app.riesgo.ui.screens.auth.RegisterScreen
import com.app.riesgo.ui.screens.history.HistoryScreen
import com.app.riesgo.ui.screens.imc.ImcScreen
import com.app.riesgo.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Form : Screen("form")
    object InjurySurvey : Screen("injury_survey")
    object Selection : Screen("selection")
    object CameraTest : Screen("camera_test/{testId}") {
        fun createRoute(testId: String) = "camera_test/$testId"
    }
    object PostTestFeedbackRoute : Screen("post_test_feedback")
    object Results : Screen("results/{recordId}") {
        fun createRoute(recordId: String) = "results/$recordId"
    }
    object History : Screen("history")
    object Imc : Screen("imc")
    object Profile : Screen("profile")
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Form.route, "Evaluación", Icons.Default.FitnessCenter),
    BottomNavItem(Screen.History.route, "Historial", Icons.Default.History),
    BottomNavItem(Screen.Imc.route, "IMC OMS", Icons.Default.MonitorWeight),
    BottomNavItem(Screen.Profile.route, "Perfil", Icons.Default.Person)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // Muestra BottomBar solo en las pestañas principales
    val showBottomBar = currentRoute in listOf(
        Screen.Form.route,
        Screen.History.route,
        Screen.Imc.route,
        Screen.Profile.route
    )

    // Estado compartido para el flujo activo
    var currentEdad by remember { mutableIntStateOf(25) }
    var currentPeso by remember { mutableFloatStateOf(70f) }
    var currentAltura by remember { mutableFloatStateOf(175f) }
    var currentLesiones by remember { mutableStateOf(false) }

    var activeTest by remember { mutableStateOf<PhysicalTest?>(null) }
    var activeCameraMetrics by remember { mutableStateOf(CameraBiomechanicalMetrics()) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (AppRepository.currentUser != null) Screen.Form.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Form.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Form.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.Form.route) {
                FormScreen(
                    onNavigateToSelection = { edad, peso, altura, tieneLesiones ->
                        currentEdad = edad
                        currentPeso = peso
                        currentAltura = altura
                        currentLesiones = tieneLesiones
                        navController.navigate(Screen.Selection.route)
                    },
                    onNavigateToSurvey = {
                        navController.navigate(Screen.InjurySurvey.route)
                    },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.InjurySurvey.route) {
                InjurySurveyScreen(
                    onComplete = {
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Selection.route) {
                SelectionScreen(
                    onSelectTest = { test ->
                        activeTest = test
                        navController.navigate(Screen.CameraTest.createRoute(test.id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CameraTest.route,
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) {
                CameraExerciseScreen(
                    testName = activeTest?.nombre ?: "Prueba de Sentadilla",
                    onFinishCameraTest = { metrics ->
                        activeCameraMetrics = metrics
                        navController.navigate(Screen.PostTestFeedbackRoute.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PostTestFeedbackRoute.route) {
                PostTestFeedbackScreen(
                    cameraMetrics = activeCameraMetrics,
                    onCompleteFeedback = { feedback ->
                        val user = AppRepository.currentUser
                        val evalEdad = user?.edad ?: currentEdad
                        val evalPeso = user?.peso ?: currentPeso
                        val evalAltura = user?.altura ?: currentAltura
                        val imc = InjuryRiskEngine.calculateImc(evalPeso, evalAltura)
                        val userSport = user?.deporte ?: "Fútbol"
                        val sportType = SportType.fromString(userSport)
                        val testName = activeTest?.nombre ?: "Sentadilla Profunda Overhead"
                        val testId = activeTest?.id ?: "squat"

                        val analysis = InjuryRiskEngine.calculateSportRisk(
                            deporte = sportType,
                            prueba = testId,
                            edad = evalEdad,
                            imc = imc,
                            tieneLesionesPrevias = currentLesiones,
                            surveyResult = AppRepository.currentSurveyResult,
                            cameraMetrics = activeCameraMetrics,
                            postTestFeedback = feedback
                        )

                        val saved = AppRepository.saveEvaluation(
                            tipoPrueba = testName,
                            deporteEvaluado = sportType.displayName,
                            edad = evalEdad,
                            peso = evalPeso,
                            altura = evalAltura,
                            imc = imc,
                            nivelRiesgo = analysis.overallRiskLevel,
                            probabilidad = analysis.riskPercentage / 100f,
                            factores = analysis.biomechanicalFactors,
                            recomendaciones = analysis.targetedRecommendations,
                            zonasCriticas = analysis.criticalZones.map { zone ->
                                ZoneRiskDto(
                                    zoneName = zone.zoneName,
                                    category = zone.category,
                                    alertLevel = zone.alertLevel.name,
                                    description = zone.description
                                )
                            },
                            surveyResult = AppRepository.currentSurveyResult,
                            tiempoSostenidoIzquierdo = activeCameraMetrics.tiempoSostenidoIzquierdo,
                            tiempoSostenidoDerecho = activeCameraMetrics.tiempoSostenidoDerecho,
                            indiceOscilacion = activeCameraMetrics.indiceOscilacion
                        )

                        val recordWithCamera = saved.copy(
                            cameraMetrics = activeCameraMetrics,
                            postTestFeedback = feedback
                        )

                        scope.launch {
                            EvaluationRepository.saveEvaluation(recordWithCamera)
                        }

                        navController.navigate(Screen.Results.createRoute(saved.id)) {
                            popUpTo(Screen.Form.route)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Results.route,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId")
                val record = recordId?.let { AppRepository.getEvaluationById(it) }

                if (record != null) {
                    ResultsScreen(
                        record = record,
                        onFinish = {
                            navController.navigate(Screen.Form.route) {
                                popUpTo(Screen.Form.route) { inclusive = true }
                            }
                        },
                        onViewHistory = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Form.route)
                            }
                        }
                    )
                }
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNewAssessment = {
                        navController.navigate(Screen.Form.route) {
                            popUpTo(Screen.Form.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onSelectRecord = { recordId ->
                        navController.navigate(Screen.Results.createRoute(recordId))
                    }
                )
            }

            composable(Screen.Imc.route) {
                ImcScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
