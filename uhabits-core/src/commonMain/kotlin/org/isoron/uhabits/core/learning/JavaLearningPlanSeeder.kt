/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.learning

data class SeedDay(
    val dayIndex: Int,
    val title: String,
    val description: String,
    val category: String,
    val isRestDay: Boolean = false,
    val estimatedMinutes: Int = 45,
    val tasks: List<SeedTask> = emptyList()
)

data class SeedTask(
    val title: String,
    val description: String = "",
    val deliverable: String = ""
)

object JavaLearningPlanSeeder {

    fun seedDays(): List<SeedDay> = listOf(
        SeedDay(
            1, "Set up tracker and environment",
            "Assess current knowledge and prepare your Java development environment.",
            "Getting Started",
            tasks = listOf(
                SeedTask("Install JDK and IDE", "Confirm javac and a project can run.", "HelloWorld compiles and runs"),
                SeedTask("Assess baseline", "List topics you already know vs need to learn.", "Short self-assessment notes"),
                SeedTask("Configure this tracker", "Set reminder time and daily target.", "Reminder enabled for study days")
            )
        ),
        SeedDay(
            2, "Classes and objects — BankAccount",
            "Build a BankAccount class with deposit and withdraw behavior.",
            "OOP",
            tasks = listOf(
                SeedTask("Explain classes vs objects", deliverable = "3–5 sentence explanation"),
                SeedTask("Implement BankAccount", "Fields: balance; methods: deposit, withdraw.", "Working BankAccount class"),
                SeedTask("Write a small demo main", deliverable = "Demo creates an account and prints balance"),
                SeedTask("Note unclear points", deliverable = "List of questions")
            )
        ),
        SeedDay(
            3, "Encapsulation and valid state",
            "Use private fields and guard invalid balances.",
            "OOP",
            tasks = listOf(
                SeedTask("Make fields private", deliverable = "No public mutable balance"),
                SeedTask("Validate withdraw/deposit", "Reject negative amounts.", "Tests or manual checks"),
                SeedTask("Explain encapsulation", deliverable = "Plain-English summary")
            )
        ),
        SeedDay(
            4, "Constructors and this",
            "Practice constructors and the this keyword.",
            "OOP",
            tasks = listOf(
                SeedTask("Add constructors", "Default and parameterized.", "Two constructors on BankAccount"),
                SeedTask("Use this correctly", deliverable = "Example using this for field assignment"),
                SeedTask("Independent variation", "Add account holder name.", "Updated class")
            )
        ),
        SeedDay(
            5, "Access modifiers and packages",
            "Understand public, private, protected, package-private and packages.",
            "OOP",
            tasks = listOf(
                SeedTask("Explain each modifier", deliverable = "Table or short notes"),
                SeedTask("Place class in a package", deliverable = "Package declaration works"),
                SeedTask("Try an access mistake", "See what the compiler rejects.", "Notes on errors")
            )
        ),
        SeedDay(
            6, "Week 1 review — rebuild BankAccount",
            "Rebuild BankAccount without following a tutorial.",
            "OOP",
            tasks = listOf(
                SeedTask("Rebuild from memory", deliverable = "Fresh BankAccount implementation"),
                SeedTask("List weak spots", deliverable = "Topics to revisit"),
                SeedTask("Solve one variation", deliverable = "e.g. overdraft limit")
            )
        ),
        SeedDay(7, "Rest or catch up", "Optional rest or finish unfinished Week 1 work.", "Rest", isRestDay = true, estimatedMinutes = 0),
        SeedDay(
            8, "Inheritance",
            "Model an inheritance hierarchy with a shared base class.",
            "OOP",
            tasks = listOf(
                SeedTask("Explain inheritance", deliverable = "Own-words summary"),
                SeedTask("Implement a subclass", "e.g. SavingsAccount extends BankAccount.", "Working subclass"),
                SeedTask("Independent example", deliverable = "Small hierarchy of your choice")
            )
        ),
        SeedDay(
            9, "Overriding and polymorphism",
            "Override methods and use polymorphic references.",
            "OOP",
            tasks = listOf(
                SeedTask("Override a method", deliverable = "Overridden toString or withdraw"),
                SeedTask("Demonstrate polymorphism", deliverable = "Base-type reference to subclass"),
                SeedTask("Note common mistakes", deliverable = "Short checklist")
            )
        ),
        SeedDay(
            10, "Overloading vs overriding",
            "Contrast method overloading and overriding clearly.",
            "OOP",
            tasks = listOf(
                SeedTask("Write overloaded methods", deliverable = "Two+ overloads"),
                SeedTask("Explain the difference", deliverable = "Comparison in your words"),
                SeedTask("Solve a small quiz yourself", deliverable = "Answers without lookup")
            )
        ),
        SeedDay(
            11, "Abstract classes",
            "Create and use an abstract class.",
            "OOP",
            tasks = listOf(
                SeedTask("Define an abstract class", deliverable = "Abstract type with one abstract method"),
                SeedTask("Implement concrete subclass", deliverable = "Working subclass"),
                SeedTask("When to use abstract vs interface", deliverable = "Decision notes")
            )
        ),
        SeedDay(
            12, "Interfaces",
            "Define and implement interfaces.",
            "OOP",
            tasks = listOf(
                SeedTask("Create an interface", deliverable = "Interface with 1–2 methods"),
                SeedTask("Implement it in a class", deliverable = "Concrete implementation"),
                SeedTask("Explain trade-offs", deliverable = "Notes vs abstract classes")
            )
        ),
        SeedDay(
            13, "OOP review — independent mini project",
            "Implement a small OOP example without a tutorial.",
            "OOP",
            tasks = listOf(
                SeedTask("Design classes on paper", deliverable = "Brief design notes"),
                SeedTask("Implement independently", deliverable = "Compiling mini project"),
                SeedTask("Mark weak topics", deliverable = "Needs-review list")
            )
        ),
        SeedDay(14, "Rest or catch up", "Optional rest or finish Week 2 work.", "Rest", isRestDay = true, estimatedMinutes = 0),
        SeedDay(
            15, "Collections overview",
            "Know when to use List, Set, Map, and Queue.",
            "Collections",
            tasks = listOf(
                SeedTask("Summarize collection types", deliverable = "When-to-use notes"),
                SeedTask("Sketch examples", deliverable = "One use case each"),
                SeedTask("List unknowns", deliverable = "Questions for later days")
            )
        ),
        SeedDay(
            16, "ArrayList basics",
            "Practice ArrayList operations.",
            "Collections",
            tasks = listOf(
                SeedTask("Explain ArrayList", deliverable = "Own-words summary"),
                SeedTask("Implement CRUD ops", "add, get, remove, iterate.", "Working snippets"),
                SeedTask("Solve a small problem", deliverable = "e.g. filter list of numbers")
            )
        ),
        SeedDay(
            17, "HashMap and frequency counting",
            "Use HashMap for frequency counting.",
            "Collections",
            tasks = listOf(
                SeedTask("Explain HashMap", deliverable = "Own-words summary"),
                SeedTask("Frequency counter", deliverable = "Count word/char frequencies"),
                SeedTask("Solve a variation without copying", deliverable = "Independent solution"),
                SeedTask("Record unclear points", deliverable = "Questions list")
            )
        ),
        SeedDay(
            18, "HashSet and deduplication",
            "Use HashSet to remove duplicates.",
            "Collections",
            tasks = listOf(
                SeedTask("Explain HashSet", deliverable = "Own-words summary"),
                SeedTask("Deduplicate a list", deliverable = "Working example"),
                SeedTask("Contrast with List", deliverable = "Trade-off notes")
            )
        ),
        SeedDay(
            19, "equals and hashCode",
            "Implement equals and hashCode correctly for map/set keys.",
            "Collections",
            tasks = listOf(
                SeedTask("Explain the contract", deliverable = "Rules summary"),
                SeedTask("Implement for a domain class", deliverable = "equals + hashCode"),
                SeedTask("Show a HashMap key bug", "Broken hashCode then fix.", "Before/after notes")
            )
        ),
        SeedDay(
            20, "Expense or debt category summary",
            "Build a small collections-based summary tool.",
            "Collections",
            tasks = listOf(
                SeedTask("Model the data", deliverable = "Simple classes"),
                SeedTask("Aggregate with collections", deliverable = "Category totals"),
                SeedTask("Independent stretch", deliverable = "Extra feature of your choice")
            )
        ),
        SeedDay(21, "Rest or catch up", "Optional rest or finish Week 3 work.", "Rest", isRestDay = true, estimatedMinutes = 0),
        SeedDay(
            22, "Exceptions and custom exceptions",
            "Handle errors with try/catch and custom exception types.",
            "Modern Java",
            tasks = listOf(
                SeedTask("Explain checked vs unchecked", deliverable = "Notes"),
                SeedTask("Create a custom exception", deliverable = "Custom type used in code"),
                SeedTask("Practice meaningful handling", deliverable = "Example with recovery path")
            )
        ),
        SeedDay(
            23, "Generics",
            "Write and use generic types and methods.",
            "Modern Java",
            tasks = listOf(
                SeedTask("Explain type parameters", deliverable = "Own-words summary"),
                SeedTask("Implement a generic box/holder", deliverable = "Generic class"),
                SeedTask("Use generics with collections", deliverable = "Typed List/Map example")
            )
        ),
        SeedDay(
            24, "Immutability",
            "Design immutable data holders.",
            "Modern Java",
            tasks = listOf(
                SeedTask("Explain benefits", deliverable = "Short notes"),
                SeedTask("Create an immutable class", deliverable = "Final fields, no setters"),
                SeedTask("Identify mutable pitfalls", deliverable = "Checklist")
            )
        ),
        SeedDay(
            25, "Lambdas and functional interfaces",
            "Use lambdas with common functional interfaces.",
            "Modern Java",
            tasks = listOf(
                SeedTask("Explain lambda syntax", deliverable = "Own-words summary"),
                SeedTask("Use Predicate/Function/Consumer", deliverable = "Three small examples"),
                SeedTask("Refactor a loop to lambda", deliverable = "Before/after snippet")
            )
        ),
        SeedDay(
            26, "Stream API basics",
            "Filter, map, and collect with streams.",
            "Modern Java",
            tasks = listOf(
                SeedTask("Explain stream pipeline", deliverable = "Notes"),
                SeedTask("filter/map/collect practice", deliverable = "Working examples"),
                SeedTask("Solve one problem with streams", deliverable = "Independent solution")
            )
        ),
        SeedDay(
            27, "JUnit basics",
            "Write tests that assert behavior.",
            "Testing",
            tasks = listOf(
                SeedTask("Add a JUnit test class", deliverable = "At least two tests"),
                SeedTask("Test happy path and edge case", deliverable = "Passing tests"),
                SeedTask("Explain Arrange-Act-Assert", deliverable = "Short notes")
            )
        ),
        SeedDay(
            28, "Debt-tracker domain model",
            "Model Debt, Payment, and balance calculation.",
            "Project",
            tasks = listOf(
                SeedTask("Design Debt and Payment", deliverable = "Class sketch"),
                SeedTask("Implement balance calculation", deliverable = "Working domain model"),
                SeedTask("Write basic tests", deliverable = "JUnit coverage for balance")
            )
        ),
        SeedDay(
            29, "Rebuild without tutorial + weak topics",
            "Rebuild a previous example from memory and list weak topics.",
            "Review",
            tasks = listOf(
                SeedTask("Rebuild an earlier example", deliverable = "Independent implementation"),
                SeedTask("List weak topics", deliverable = "Prioritized review list"),
                SeedTask("Schedule revisions", deliverable = "Topics marked Needs Review")
            )
        ),
        SeedDay(
            30, "Month review and next plan",
            "Review the month and decide the next learning plan.",
            "Review",
            tasks = listOf(
                SeedTask("Summarize wins", deliverable = "What you can explain/implement"),
                SeedTask("Identify gaps", deliverable = "Next-plan topics"),
                SeedTask("Draft next 2-week focus", deliverable = "Concrete next priorities")
            )
        )
    )

    fun ensureSeeded(repository: LearningRepository, startDate: String, nowMillis: Long): LearningPlan {
        repository.findActivePlan()?.let { return it }

        val plan = LearningPlan(
            name = "Java Backend 30-Day Plan",
            startDate = startDate,
            currentDayIndex = 1,
            status = PlanStatus.ACTIVE,
            createdAt = nowMillis
        )
        val planId = repository.insertPlan(plan)

        for (seed in seedDays()) {
            val day = LearningDay(
                planId = planId,
                dayIndex = seed.dayIndex,
                title = seed.title,
                description = seed.description,
                estimatedMinutes = seed.estimatedMinutes,
                isRestDay = seed.isRestDay,
                status = if (seed.dayIndex == 1) DayStatus.IN_PROGRESS else DayStatus.PENDING
            )
            val dayId = repository.insertDay(day)
            seed.tasks.forEachIndexed { index, t ->
                repository.insertTask(
                    LearningTask(
                        dayId = dayId,
                        position = index,
                        title = t.title,
                        description = t.description,
                        deliverable = t.deliverable
                    )
                )
            }
            if (repository.findTopicByName(seed.title) == null && !seed.isRestDay) {
                repository.insertTopic(
                    LearningTopic(
                        name = seed.title,
                        category = seed.category,
                        status = TopicStatus.TO_LEARN
                    )
                )
            }
        }
        return plan
    }
}
