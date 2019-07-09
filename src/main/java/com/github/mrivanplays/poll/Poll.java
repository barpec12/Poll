package com.github.mrivanplays.poll;

import java.util.function.Function;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.mrivanplays.poll.commands.CommandPoll;
import com.github.mrivanplays.poll.commands.CommandPollSend;
import com.github.mrivanplays.poll.commands.CommandPollVotes;
import com.github.mrivanplays.poll.question.QuestionAnnouncer;
import com.github.mrivanplays.poll.question.QuestionHandler;
import com.github.mrivanplays.poll.storage.SerializableQuestion;
import com.github.mrivanplays.poll.storage.SerializableQuestions;
import com.github.mrivanplays.poll.storage.VotersFile;
import com.github.mrivanplays.poll.util.MetricsSetup;
import com.github.mrivanplays.poll.util.UpdateChecker;

public final class Poll extends JavaPlugin
{

    @Getter
    private VotersFile votersFile;
    @Getter
    private QuestionHandler questionHandler;
    private QuestionAnnouncer announcer;

    public static Function<String, String> ANSWER_FUNCTION = answer ->
    {
        char[] chars = answer.toCharArray();
        for ( int i = 0; i < chars.length; i++ )
        {
            if ( chars[i] == '&' || chars[i] == '\u00a7' )
            {
                chars[i] = ' ';
                chars[i + 1] = ' ';
            }
        }
        String s = new String( chars );
        return s.replace( " ", "" );
    };

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        votersFile = new VotersFile( getDataFolder() );
        if ( !votersFile.deserialize().isEmpty() )
        {
            for ( SerializableQuestion question : votersFile.deserialize() )
            {
                SerializableQuestions.register( question );
            }
        }
        questionHandler = new QuestionHandler( this );
        new CommandPoll( this );
        new CommandPollVotes( this );
        new CommandPollSend( this );
        announcer = new QuestionAnnouncer( this );
        announcer.loadAsAnnouncements();
        new MetricsSetup( this ).setup();
        getLogger().info( "Plugin enabled" );
        // The task is executed on another thread
        // Starts after 5 minutes and repeats every 30 minutes
        // Can't harm the server while saving things into a file.
        getServer().getScheduler().runTaskTimerAsynchronously( this, () ->
                votersFile.serialize( SerializableQuestions.getForSerialize() ), 300 * 20, 600 * 3 * 20 );
        if ( getConfig().getBoolean( "update-check" ) )
        {
            new UpdateChecker( this, 69153, "poll.updatenotify" );
        }
    }

    @Override
    public void onDisable()
    {
        votersFile.serialize( SerializableQuestions.getForSerialize() );
        getLogger().info( "Plugin disabled" );
    }

    public String color(String text)
    {
        return ChatColor.translateAlternateColorCodes( '&', text );
    }

    public void reload()
    {
        reloadConfig();
        announcer.reloadAnnouncements();
        votersFile.serialize( SerializableQuestions.getForSerialize() );
    }
}
