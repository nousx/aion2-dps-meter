const createMeterUI = ({ elList, dpsFormatter, getUserName, onClickUserRow, getMetric, formatNumber }) => {
  const MAX_CACHE = 32;

  // Use formatNumber if provided, otherwise use dpsFormatter
  const fmt = formatNumber || ((n) => dpsFormatter.format(Math.floor(n)));

  const rowViewById = new Map();
  let lastVisibleIds = new Set();

  const nowMs = () => (typeof performance !== "undefined" ? performance.now() : Date.now());

  const createRowView = (id) => {
    const rowEl = document.createElement("div");
    rowEl.className = "item";
    rowEl.style.display = "none";
    rowEl.dataset.rowId = String(id);

    const fillEl = document.createElement("div");
    fillEl.className = "fill";

    const contentEl = document.createElement("div");
    contentEl.className = "content";

    const classIconEl = document.createElement("div");
    classIconEl.className = "classIcon";

    const classIconImg = document.createElement("img");
    classIconImg.className = "classIconImg";
    classIconImg.style.visibility = "hidden";

    classIconImg.draggable = false;

    classIconEl.appendChild(classIconImg);

    const rankEl = document.createElement("div");
    rankEl.className = "rank";

    const nameEl = document.createElement("div");
    nameEl.className = "name";

    const damageEl = document.createElement("div");
    damageEl.className = "damage";

    const dpsEl = document.createElement("div");
    dpsEl.className = "dps";

    const critEl = document.createElement("div");
    critEl.className = "crit";

    const contributionEl = document.createElement("div");
    contributionEl.className = "contribution";

    const diffEl = document.createElement("div");
    diffEl.className = "diff";

    contentEl.appendChild(rankEl);
    contentEl.appendChild(classIconEl);
    contentEl.appendChild(nameEl);
    contentEl.appendChild(damageEl);
    contentEl.appendChild(dpsEl);
    contentEl.appendChild(critEl);
    contentEl.appendChild(contributionEl);
    contentEl.appendChild(diffEl);
    rowEl.appendChild(fillEl);
    rowEl.appendChild(contentEl);

    const view = {
      id,
      rowEl,
      prevContribClass: "",
      rankEl,
      nameEl,
      damageEl,
      dpsEl,
      critEl,
      contributionEl,
      diffEl,
      classIconEl,
      classIconImg,
      fillEl,
      currentRow: null,
      lastSeenAt: 0,
    };

    rowEl.addEventListener("click", () => {
      // if (view.currentRow?.isUser)
      onClickUserRow?.(view.currentRow);
    });

    return view;
  };

  const getRowView = (id) => {
    let view = rowViewById.get(id);
    if (!view) {
      view = createRowView(id);
      rowViewById.set(id, view);
      elList.appendChild(view.rowEl);
    }
    return view;
  };

  // 상위 6개 + 유저(유저가 top6 밖이면 7개)
  const getDisplayRows = (sortedAll) => {
    const top6 = sortedAll.slice(0, 6);
    const user = sortedAll.find((x) => x.isUser);

    if (!user) return top6;
    if (top6.some((x) => x.isUser)) return top6;
    return [...top6, user];
  };

  const pruneCache = (keepIds) => {
    if (rowViewById.size <= MAX_CACHE) return;

    const candidates = [];
    for (const [id, view] of rowViewById) {
      if (keepIds.has(id)) {
        continue;
      }
      candidates.push({ id, t: view.lastSeenAt || 0 });
    }

    candidates.sort((a, b) => a.t - b.t); // 오래된거 제거

    for (let i = 0; rowViewById.size > MAX_CACHE && i < candidates.length; i++) {
      const id = candidates[i].id;
      const view = rowViewById.get(id);
      if (!view) continue;
      view.rowEl.remove();
      rowViewById.delete(id);
    }
  };

  const resolveMetric = (row) => {
    if (typeof getMetric === "function") {
      return getMetric(row);
    }
    const dps = Number(row?.dps) || 0;
    return { value: dps, text: `${dpsFormatter.format(dps)}` };
  };

  const renderRows = (rows) => {
    const now = nowMs();
    const nextVisibleIds = new Set();

    elList.classList.toggle("hasRows", rows.length > 0);

    let topDps = 1;
    let topDamage = 1;
    for (const row of rows) {
      const dps = Number(row?.dps) || 0;
      const damage = Number(row?.damage) || 0;
      topDps = Math.max(topDps, dps);
      topDamage = Math.max(topDamage, damage);
    }

    let rank = 0;
    for (const row of rows) {
      if (!row) {
        continue;
      }

      const id = row.id ?? row.name;
      if (!id) {
        continue;
      }

      nextVisibleIds.add(id);
      rank++;

      const view = getRowView(id);
      view.currentRow = row;
      view.lastSeenAt = now;

      view.rowEl.style.display = "";
      view.rowEl.classList.toggle("isUser", !!row.isUser);
      view.nameEl.classList.toggle("noNickname", !row.hasNickname);

      view.rankEl.textContent = rank;
      view.nameEl.textContent = row.name ?? "";

      // Debug: check job value
      if (row.job && row.job.trim() !== "") {
        const src = `assets/classes/${row.job}.png`;
        console.log(`Loading icon for ${row.name}: job="${row.job}" → ${src}`);
        view.classIconImg.src = src;
        view.classIconImg.style.visibility = "visible";
        view.classIconImg.onload = () => {
          console.log(`✓ Icon loaded: ${src}`);
        };
        view.classIconImg.onerror = () => {
          console.error(`✗ Failed to load icon: ${src}, job="${row.job}", charCodes: ${Array.from(row.job).map(c => c.charCodeAt(0)).join(',')}`);
          view.classIconImg.style.visibility = "hidden";
        };
      } else {
        console.log(`No job for ${row.name}: job="${row.job}"`);
        view.classIconImg.style.visibility = "hidden";
      }

      const damage = Number(row.totalDamage) || 0;
      const dps = Number(row.dps) || 0;
      const burstDps = Number(row.burstDps) || 0;
      const crit = Number(row.totalCritPct) || 0;
      const back = Number(row.totalBackPct) || 0;
      const damageContribution = Number(row.damageContribution) || 0;

      // Calculate diff % from rank 1 (top damage)
      const diffPercent = topDamage > 0 ? ((damage - topDamage) / topDamage) * 100 : 0;

      let contributionClass = "";
      if (damageContribution < 3) {
        contributionClass = "error";
      } else if (damageContribution < 5) {
        contributionClass = "warning";
      }
      if (view.prevContribClass !== contributionClass) {
        if (view.prevContribClass) {
          view.rowEl.classList.remove(view.prevContribClass);
        }
        if (contributionClass) {
          view.rowEl.classList.add(contributionClass);
        }
        view.prevContribClass = contributionClass;
      }

      // Smooth number interpolation
      const interpolator = window.numberInterpolator;
      if (interpolator) {
        const damageKey = `damage-${id}`;
        const dpsKey = `dps-${id}`;
        const contribKey = `contrib-${id}`;
        const barKey = `bar-${id}`;

        const smoothDamage = interpolator.setValue(damageKey, damage);
        const smoothDps = interpolator.setValue(dpsKey, dps);
        const smoothContrib = interpolator.setValue(contribKey, damageContribution);
        const smoothRatio = interpolator.setValue(barKey, Math.max(0, Math.min(1, dps / topDps)));

        // Update with interpolated values
        view.damageEl.textContent = fmt(smoothDamage);

        // Show burst DPS indicator if significantly higher than sustained
        const hasBurst = burstDps > smoothDps * 1.3;
        if (hasBurst && burstDps > 0) {
          view.dpsEl.textContent = fmt(smoothDps) + " ";
          const burstSpan = document.createElement("span");
          burstSpan.className = "burstDps";
          burstSpan.textContent = `⚡${fmt(burstDps)}`;
          view.dpsEl.appendChild(burstSpan);
          view.dpsEl.title = `Sustained: ${fmt(smoothDps)}\nBurst (1s): ${fmt(burstDps)}`;
        } else {
          view.dpsEl.textContent = fmt(smoothDps);
          view.dpsEl.title = "";
        }

        // Show crit and back attack rates
        const critText = crit > 0 ? `${crit.toFixed(1)}%` : "-";
        if (back > 5) {
          view.critEl.textContent = critText + " ";
          const backSpan = document.createElement("span");
          backSpan.className = "backAttack";
          backSpan.textContent = `| ${back.toFixed(0)}%`;
          view.critEl.appendChild(backSpan);
          view.critEl.title = `Crit: ${crit.toFixed(1)}%\nBack: ${back.toFixed(1)}%`;
        } else {
          view.critEl.textContent = critText;
          view.critEl.title = "";
        }

        view.contributionEl.textContent = `${smoothContrib.toFixed(1)}%`;

        // Show diff for rank > 1
        if (diffPercent < -0.1) {
          view.diffEl.textContent = `${diffPercent.toFixed(1)}%`;
          view.diffEl.style.display = "";
        } else {
          view.diffEl.textContent = "-";
          view.diffEl.style.display = "";
        }

        // Dot is always visible, optionally adjust opacity based on DPS
        view.fillEl.style.opacity = Math.max(0.3, smoothRatio);
      } else {
        // Fallback if interpolator not available
        view.damageEl.textContent = fmt(damage);

        // Show burst DPS indicator if significantly higher than sustained
        const hasBurst = burstDps > dps * 1.3;
        if (hasBurst && burstDps > 0) {
          view.dpsEl.textContent = fmt(dps) + " ";
          const burstSpan = document.createElement("span");
          burstSpan.className = "burstDps";
          burstSpan.textContent = `⚡${fmt(burstDps)}`;
          view.dpsEl.appendChild(burstSpan);
          view.dpsEl.title = `Sustained: ${fmt(dps)}\nBurst (1s): ${fmt(burstDps)}`;
        } else {
          view.dpsEl.textContent = fmt(dps);
          view.dpsEl.title = "";
        }

        // Show crit and back attack rates
        const critText = crit > 0 ? `${crit.toFixed(1)}%` : "-";
        if (back > 5) {
          view.critEl.textContent = critText + " ";
          const backSpan = document.createElement("span");
          backSpan.className = "backAttack";
          backSpan.textContent = `| ${back.toFixed(0)}%`;
          view.critEl.appendChild(backSpan);
          view.critEl.title = `Crit: ${crit.toFixed(1)}%\nBack: ${back.toFixed(1)}%`;
        } else {
          view.critEl.textContent = critText;
          view.critEl.title = "";
        }

        view.contributionEl.textContent = `${damageContribution.toFixed(1)}%`;

        if (diffPercent < -0.1) {
          view.diffEl.textContent = `${diffPercent.toFixed(1)}%`;
          view.diffEl.style.display = "";
        } else {
          view.diffEl.textContent = "-";
          view.diffEl.style.display = "";
        }

        // Dot is always visible, optionally adjust opacity based on DPS
        const ratio = Math.max(0, Math.min(1, dps / topDps));
        view.fillEl.style.opacity = Math.max(0.3, ratio);
      }

      elList.appendChild(view.rowEl);
    }

    for (const id of lastVisibleIds) {
      if (nextVisibleIds.has(id)) continue;
      const view = rowViewById.get(id);
      if (view) view.rowEl.style.display = "none";
    }

    lastVisibleIds = nextVisibleIds;

    pruneCache(nextVisibleIds);
  };

  const updateFromRows = (rows) => {
    const arr = Array.isArray(rows) ? rows.slice() : [];
    arr.sort((a, b) => {
      const aDps = Number(a?.dps) || 0;
      const bDps = Number(b?.dps) || 0;
      return bDps - aDps;
    });
    renderRows(getDisplayRows(arr));
  };
  const onResetMeterUi = () => {
    elList.classList.remove("hasRows");
    lastVisibleIds = new Set();

    const battleTimeEl = elList.querySelector(".battleTime");
    if (battleTimeEl) {
      elList.replaceChildren(battleTimeEl);
    } else {
      elList.replaceChildren();
    }

    rowViewById.clear();
  };
  return { updateFromRows, onResetMeterUi };
};
