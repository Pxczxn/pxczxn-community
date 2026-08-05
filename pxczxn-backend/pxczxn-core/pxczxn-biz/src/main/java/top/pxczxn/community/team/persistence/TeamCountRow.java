package top.pxczxn.community.team.persistence;

import lombok.Getter;
import lombok.Setter;

/**
 * Aggregate count row keyed by team.
 *
 * <p>Used by dashboard and "my teams" queries so a user joining N teams still costs a single
 * grouped query per metric instead of N per-team round trips.</p>
 */
@Getter
@Setter
public class TeamCountRow {

    private Long teamId;

    private Integer total;
}
