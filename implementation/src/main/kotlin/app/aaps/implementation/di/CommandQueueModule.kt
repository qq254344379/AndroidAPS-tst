package app.aaps.implementation.di

import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.implementation.queue.CommandQueueImplementation
import app.aaps.implementation.queue.CommandQueueName
import app.aaps.implementation.queue.QueueWorker
import app.aaps.implementation.queue.commands.CommandBolus
import app.aaps.implementation.queue.commands.CommandReadStatus
import app.aaps.implementation.queue.commands.CommandSMBBolus
import app.aaps.implementation.queue.commands.CommandSetProfile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module(
    includes = [
        CommandQueueModule.Bindings::class
    ]
)
@InstallIn(SingletonComponent::class)
open class CommandQueueModule {

    @Suppress("unused")
    @Module
    @InstallIn(SingletonComponent::class)
    interface Bindings {

        @Binds fun bindCommandQueueInjector(commandQueueImplementation: CommandQueueImplementation): CommandQueue

        @ContributesAndroidInjector fun queueWorkerInjector(): QueueWorker
        @ContributesAndroidInjector fun commandBolusInjector(): CommandBolus
        @ContributesAndroidInjector fun commandReadStatusInjector(): CommandReadStatus
        @ContributesAndroidInjector fun commandSetProfileInjector(): CommandSetProfile
        @ContributesAndroidInjector fun commandCommandSMBBolusInjector(): CommandSMBBolus
    }

    @Provides
    fun commandQueueJobName(): CommandQueueName = CommandQueueName("CommandQueue")
}