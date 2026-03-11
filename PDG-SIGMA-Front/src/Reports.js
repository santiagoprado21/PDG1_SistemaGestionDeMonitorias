import './Reports.css';
import React, { useEffect, useState, useMemo } from 'react';
import VerticalNavbar from './VerticalNavbar';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  PieChart, Pie, Cell,
  LabelList
} from 'recharts';
import {PopUp} from "./PopUp";
import { BACKEND_URL, getApiUrl } from './config/ApiBackend';

const REPORT_HELP_CONTENT = {
  monitorPerformance: {
    summary: 'Muestra el avance de actividades por monitor para los filtros seleccionados.',
    bullets: [
      'Barras verdes: completadas, amarillas: tardias, rojas: pendientes.',
      'Usa el orden A-Z / Z-A para ubicar monitores rapidamente.',
      'El tooltip del grafico indica cantidad y porcentaje por estado.'
    ]
  },
  semesterComparison: {
    summary: 'Compara el desempeno del semestre actual frente al semestre anterior disponible.',
    bullets: [
      'Se requiere al menos dos semestres con datos para ver la comparacion.',
      'Cada barra representa actividades completadas, pendientes y tardias.',
      'Si cambias filtros, la comparacion se recalcula automaticamente.'
    ]
  },
  monitorTaskSummary: {
    summary: 'Resume en porcentaje el estado total de tareas de monitores.',
    bullets: [
      'Completadas: actividades finalizadas dentro del periodo.',
      'Completadas tardias: cerradas despues de la fecha esperada.',
      'Pendientes: actividades que aun no se han completado.'
    ]
  },
  categoryUsage: {
    summary: 'Presenta las categorias mas usadas en las actividades del curso filtrado.',
    bullets: [
      'Solo aparece cuando seleccionas curso, programa y semestre.',
      'Cada porcion del pastel indica la proporcion de uso por categoria.',
      'Se muestran las 5 categorias con mayor cantidad de registros.'
    ]
  },
  professorPerformance: {
    summary: 'Mide el avance de actividades asociadas a cada profesor y curso.',
    bullets: [
      'En perfil profesor se muestra el rendimiento propio.',
      'Usa los filtros para comparar cursos o periodos especificos.',
      'El tooltip muestra cantidad y porcentaje por estado de actividad.'
    ]
  },
  professorTaskSummary: {
    summary: 'Consolida en porcentaje el estado de tareas relacionadas con profesores.',
    bullets: [
      'Permite detectar carga pendiente y oportunidad de mejora.',
      'Las tarjetas se actualizan segun los filtros aplicados.',
      'Puedes exportar el resumen en CSV o PDF para seguimiento.'
    ]
  }
};

function Reports() {

  const [monitorPerformanceDataOriginal, setMonitorPerformanceDataOriginal] = useState([]);
  const [professorData, setProfessorData] = useState([]);
  const [professorDataOriginal, setProfessorDataOriginal] = useState([]);

  // console.log("Reports se está renderizando");
  const [selectedCategory, setSelectedCategory] = useState(null);
  // const [categoryUsageDataOriginal, setCategoryUsageDataOriginal] = useState([]);
  const [categoryReportData, setCategoryReportData] = useState([]);
  const [courseSelectedM, setCourseSelectedM] = useState("");
  const [courseSelectedP, setCourseSelectedP] = useState("");
  const [filteredMonitorPerformanceData, setFilteredMonitorPerformanceData] = useState([]);
  const [filteredProfessorData, setFilteredProfessorData] = useState([]);
  const [role, setRole] = useState('')

  //Pop up
  const [isOpen, setIsOpen] = useState(false)
  const [message, setMessage] = useState("")
  const [change, setChange] = useState(false)

  const [semester, setSemester] = useState('');
  const [program, setProgram] = useState('');
  const [course, setCourse] = useState('');
  const [professor, setProfessor] = useState('');
  const [monitor, setMonitor] = useState('');
  const [monitorSortOrder, setMonitorSortOrder] = useState('A-Z');
  const [openHelpKey, setOpenHelpKey] = useState(null);

  //Porcentaje efectividad por materia de monitores
  const[completedPercent, setCompletedPercent] = useState("")
  const[pendingPercent, setPendingPercent] = useState("")
  const[latePercent, setLatePercent] = useState("")
  const[porcentages, setPorcentages] = useState([{ completed: "0%", late: "0%", pending: "0%" }]);

  //Porcentaje efectividad por materia de profesores
  const[completedPercentProfessor, setCompletedPercentProfessor] = useState("")
  const[pendingPercentProfessor, setPendingPercentProfessor] = useState("")
  const[latePercentProfessor, setLatePercentProfessor] = useState("")
  const[porcentagesProfessor, setPorcentagesProfessor] = useState([{ completed: "0%", late: "0%", pending: "0%" }]);

  useEffect(() => {
    console.log("obtener---")
    setMessage("Puedes usar los filtros para refinar la información de los reportes.")
    setIsOpen(!isOpen)
    setChange(!change)
    const user = localStorage.getItem('userId');
    const role = localStorage.getItem('role');
    setRole(role);
    const fetchActivities = async () => {
      try {
        const monitorResponse = await fetch(`${BACKEND_URL}/monitoring/getMonitorsReport/${user}/${role}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': localStorage.getItem('token')
          },
        });
        if (!monitorResponse.ok) {
          const errorData = await monitorResponse.json().catch(() => ({}));
          throw new Error(errorData.error || `Error ${monitorResponse.status}`);
        }
        const monitorJson = await monitorResponse.json();
        if (!Array.isArray(monitorJson)) {
          setMonitorPerformanceDataOriginal([]);
          setProfessorDataOriginal([]);
          return;
        }
        setMonitorPerformanceDataOriginal(monitorJson);
        
        let professorList = [];
        for (const [index, report] of monitorJson.entries()) {
            let value = report.idProfessor;
            console.log('Times');
            if(!professorList.find(e => e.idProfessor === value)){
                const professorResponse = await fetch(`${BACKEND_URL}/monitoring/getProfessorReport/${value}`,{
                  method: 'GET',
                  headers: { 'Content-Type': 'application/json' ,
                      'Authorization':localStorage.getItem('token')
                  },
                  });
              const professorJson = await professorResponse.json();
              
              console.log('Value '+professorJson)
              professorList = professorList.concat(professorJson);
              
            }
        }

        setProfessorDataOriginal(professorList)
        
      } catch (error) {
        console.error("Error fetching monitor data:", error);
      }
    };

    fetchActivities();

    const fetchCategories = async () => {
      const normalizedRole = String(role || '').toLowerCase();
      let url = '';

      if (normalizedRole === 'professor') {
        url = `${BACKEND_URL}/monitoring/getCategoriesReport/professor/${user}`;
      } else if (normalizedRole === 'jfedpto') {
        url = `${BACKEND_URL}/monitoring/getCategoriesReport/jfedpto/${user}`;
      } else {
        setCategoryReportData([]);
        return;
      }

      try {
        const categoriesResponse = await fetch(url,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
            },
            });
        if (!categoriesResponse.ok) {
             const errorData = await categoriesResponse.json().catch(() => ({}));
             throw new Error(errorData.error || `Error ${categoriesResponse.status}`);
        }
        const categoriesJson = await categoriesResponse.json();
        console.log("Respuesta API Categorías:", categoriesJson); 

        setCategoryReportData(categoriesJson);

      } catch (error) {
        console.error('Error fetching categories data:', error);
        setCategoryReportData(null); 
      }
    };
    fetchCategories();
  }, []);

  const getValues = (type, activeFilters = {}) => {
    if (!Array.isArray(monitorPerformanceDataOriginal)) return [];

    const {
      semester: filterSemester = '',
      program: filterProgram = '',
      course: filterCourse = '',
      professor: filterProfessor = '',
    } = activeFilters;

    let source = monitorPerformanceDataOriginal.filter(Boolean);

    if (type !== 'semester') {
      source = source.filter(item => !filterSemester || item.semester === filterSemester);
    }

    if (type === 'courses' || type === 'professors' || type === 'monitors') {
      source = source.filter(item => !filterProgram || item.program === filterProgram);
    }

    if (type === 'professors' || type === 'monitors') {
      source = source.filter(item => !filterCourse || item.course === filterCourse);
    }

    if (type === 'monitors') {
      source = source.filter(item => !filterProfessor || item.professor === filterProfessor);
    }

    const keyByType = {
      semester: 'semester',
      programs: 'program',
      courses: 'course',
      professors: 'professor',
      monitors: 'name',
    };

    const key = keyByType[type];
    if (!key) return [];

    return [...new Set(source.map(item => item[key]).filter(Boolean))];
  };

  const sortAlphabetically = (values = []) => {
    return [...values].sort((a, b) => String(a).localeCompare(String(b), 'es', { sensitivity: 'base' }));
  };

  const sortMonitorsByOrder = (values = [], order = 'A-Z') => {
    const sorted = sortAlphabetically(values);
    return order === 'Z-A' ? sorted.reverse() : sorted;
  };

  const normalizeProfessorLabel = (value = '') => {
    return String(value)
      .replace(/\bDr\.?\b/gi, 'Profesor')
      .replace(/\s{2,}/g, ' ')
      .trim();
  };

  // const monitorPerformanceDataOriginal = [
  //   { name: 'Monitor A', Completadas: 12, Tardias: 3, Pendientes: 2, semestre: '2024-1', programa: 'Ingenieria de Sistemas', curso: 'POO', profesor: 'Claudia' },
  // ];

  const COLORS = ['#5454e9', '#4cb979', '#e4eb60', '#e9683b'];

  //  Función que aplica los filtros actuales
  const applyFilters = (data) => {
    if (!Array.isArray(data)) {
      console.warn("applyFilters recibió datos que no son un array:", data);
      return [];
    }

    return data.filter(d =>
      (!semester || d.semester === semester) &&
      (!program || d.program === program) &&
      (!course || d.course === course) &&
      (!professor || d.professor === professor) &&
      (!monitor || d.name === monitor)
    );
    
  };

  const pieChartData = useMemo(() => {
    if (!categoryReportData) {
      return [];
    }
    if(course === '' || program ==='' || semester === '') {
      return [];
    }

    if (course) {
      const courseDetail = categoryReportData.detalle_por_curso?.find(
        (detail) => detail.curso === course
      );
    
      if (courseDetail && Array.isArray(courseDetail.categorias)) {
        return courseDetail.categorias.map(cat => ({
          categoria: cat.categoria,
          cantidad_total: cat.cantidad 
        }))
        .slice(0, 5);
      } else {
        return [];
      }
    } else {
      const totals = categoryReportData?.totales_por_categoria;
      if (Array.isArray(totals)) {
        console.log("Calculando pieChartData: Devolviendo totales:", totals);
        return totals.slice(0, 5);
      } else {
        console.warn("totales_por_categoria no es un arreglo válido:", totals);
        return [];
      }
    }
    
  }, [categoryReportData, course]); 

   const exportToCSV = (data, filename, filters) => {
    if (!data || data.length === 0) return;

    const csvRows = [];

    // Tabla principal
    const headers = Object.keys(data[0]);
    csvRows.push(headers.join(','));

    data.forEach(row => {
      const values = headers.map(header => `"${row[header] ?? ''}"`);
      csvRows.push(values.join(','));
    });

    // Separación visual
    csvRows.push('', '', 'Filtros aplicados:');

    // Filtros con etiquetas
    csvRows.push('Filtro,Valor');
    csvRows.push(`Semestre,${filters.semester}`);
    csvRows.push(`Programa,${filters.program}`);
    csvRows.push(`Curso,${filters.course}`);

    if (filters.professor && filters.professor.trim() !== '') {
      csvRows.push(`Profesor,${filters.professor}`);
    }

    if (filters.monitor && filters.monitor.trim() !== '') {
      csvRows.push(`Monitor,${filters.monitor}`);
    }

    // Crear blob y descargar
    const csvString = csvRows.join('\n');
    const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = `${filename}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportToPDF = (data, filename, filters) => {
    if (!data || data.length === 0) return;

    const headers = Object.keys(data[0]);
    const tableHeaders = headers.map(header => `<th>${header}</th>`).join('');
    const tableRows = data
      .map(row => {
        const values = headers
          .map(header => {
            const rawValue = row[header];
            const value = typeof rawValue === 'object' && rawValue !== null
              ? JSON.stringify(rawValue)
              : (rawValue ?? '');
            return `<td>${value}</td>`;
          })
          .join('');
        return `<tr>${values}</tr>`;
      })
      .join('');

    const filtersRows = [
      ['Semestre', filters.semester],
      ['Programa', filters.program],
      ['Curso', filters.course],
      ['Profesor', filters.professor],
      ['Monitor', filters.monitor],
    ]
      .filter(([, value]) => value && String(value).trim() !== '')
      .map(([label, value]) => `<tr><td><strong>${label}</strong></td><td>${value}</td></tr>`)
      .join('');

    const printWindow = window.open('', '_blank');
    if (!printWindow) return;

    printWindow.document.write(`
      <html>
        <head>
          <title>${filename}</title>
          <style>
            body { font-family: 'Plus Jakarta Sans', sans-serif; padding: 20px; color: #000000; }
            h1 { margin-bottom: 12px; font-size: 22px; }
            h2 { margin-top: 18px; margin-bottom: 10px; font-size: 16px; }
            table { width: 100%; border-collapse: collapse; margin-top: 8px; }
            th, td { border: 1px solid #cecfd4; padding: 8px; font-size: 12px; text-align: left; }
            th { background: #ffffff; }
          </style>
        </head>
        <body>
          <h1>${filename}</h1>
          <table>
            <thead><tr>${tableHeaders}</tr></thead>
            <tbody>${tableRows}</tbody>
          </table>
          <h2>Filtros aplicados</h2>
          <table>
            <tbody>${filtersRows || '<tr><td colspan="2">Sin filtros adicionales</td></tr>'}</tbody>
          </table>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
  };

  //const categoryUsageData = applyFilters(categoryUsageDataOriginal);
  const semestersToShow = getValues("semester");
  const programsToShow = getValues("programs", { semester });
  const coursesToShow = getValues("courses", { semester, program });
  const professorsToShow = getValues("professors", { semester, program, course });
  const isProfessorRole = String(role || '').toLowerCase() === 'professor';
  const currentUserId = localStorage.getItem('userId');
  const ownProfessorName = useMemo(() => {
    if (!isProfessorRole || !Array.isArray(professorDataOriginal)) return '';

    const byId = professorDataOriginal.find(item => String(item.idProfessor) === String(currentUserId));
    if (byId?.name) return byId.name;

    const uniqueNames = [...new Set(professorDataOriginal.map(item => item.name).filter(Boolean))];
    return uniqueNames.length === 1 ? uniqueNames[0] : '';
  }, [isProfessorRole, professorDataOriginal, currentUserId]);
  const effectiveProfessorFilter = isProfessorRole ? ownProfessorName : professor;
  const monitorsToShow = (isProfessorRole && !ownProfessorName)
    ? []
    : getValues("monitors", { semester, program, course, professor: effectiveProfessorFilter });
  const sortedSemestersToShow = sortAlphabetically(semestersToShow);
  const sortedProgramsToShow = sortAlphabetically(programsToShow);
  const sortedCoursesToShow = sortAlphabetically(coursesToShow);
  const sortedProfessorsToShow = sortAlphabetically(professorsToShow);
  const professorChartData = useMemo(() => {
    return professorData.map(item => ({
      ...item,
      name: normalizeProfessorLabel(item.name),
      nameAndCourse: `${normalizeProfessorLabel(item.name)} ${item.course || ''}`.trim(),
    }));
  }, [professorData]);

  const parseSemesterOrder = (semesterValue) => {
    const value = String(semesterValue || '').trim();
    const match = value.match(/^(\d{4})[-\/.](\d)$/);
    if (!match) return Number.MIN_SAFE_INTEGER;
    const year = Number(match[1]);
    const period = Number(match[2]);
    if (Number.isNaN(year) || Number.isNaN(period)) return Number.MIN_SAFE_INTEGER;
    return year * 10 + period;
  };

  const semesterComparison = useMemo(() => {
    if (isProfessorRole && !ownProfessorName) {
      return {
        chartData: [],
        exportData: [],
        currentSemester: '',
        previousSemester: '',
      };
    }

    const source = Array.isArray(monitorPerformanceDataOriginal)
      ? monitorPerformanceDataOriginal.filter(item =>
          (!program || item.program === program) &&
          (!course || item.course === course) &&
          (!effectiveProfessorFilter || item.professor === effectiveProfessorFilter) &&
          (!monitor || item.name === monitor)
        )
      : [];

    const semesterSet = [...new Set(source.map(item => item.semester).filter(Boolean))];
    const orderedSemesters = semesterSet.sort((a, b) => parseSemesterOrder(a) - parseSemesterOrder(b));

    if (orderedSemesters.length === 0) {
      return {
        chartData: [],
        exportData: [],
        currentSemester: '',
        previousSemester: '',
      };
    }

    const currentSemester = orderedSemesters.includes(semester)
      ? semester
      : orderedSemesters[orderedSemesters.length - 1];

    const currentIndex = orderedSemesters.indexOf(currentSemester);
    const previousSemester = currentIndex > 0 ? orderedSemesters[currentIndex - 1] : '';

    const aggregateBySemester = (semesterValue) => {
      return source
        .filter(item => item.semester === semesterValue)
        .reduce((accumulator, item) => {
          accumulator.completed += item.completed || 0;
          accumulator.pending += item.pending || 0;
          accumulator.late += item.late || 0;
          return accumulator;
        }, { completed: 0, pending: 0, late: 0 });
    };

    const currentTotals = aggregateBySemester(currentSemester);
    const previousTotals = previousSemester
      ? aggregateBySemester(previousSemester)
      : { completed: 0, pending: 0, late: 0 };

    const chartData = [
      {
        indicador: 'Completadas',
        [currentSemester]: currentTotals.completed,
        [previousSemester || 'Semestre anterior']: previousTotals.completed,
      },
      {
        indicador: 'Pendientes',
        [currentSemester]: currentTotals.pending,
        [previousSemester || 'Semestre anterior']: previousTotals.pending,
      },
      {
        indicador: 'Tardías',
        [currentSemester]: currentTotals.late,
        [previousSemester || 'Semestre anterior']: previousTotals.late,
      },
    ];

    const exportData = chartData.map(row => ({
      Indicador: row.indicador,
      [currentSemester]: row[currentSemester],
      [previousSemester || 'Semestre anterior']: row[previousSemester || 'Semestre anterior'],
    }));

    return {
      chartData,
      exportData,
      currentSemester,
      previousSemester,
    };
  }, [monitorPerformanceDataOriginal, semester, program, course, effectiveProfessorFilter, monitor, isProfessorRole, ownProfessorName]);


  const [monitorPerformanceData, setMonitorPerformanceData] = useState([]);
  const [monitorPage, setMonitorPage] = useState(1);
  const monitorsPerPage = 10;

  const sortedMonitorPerformanceData = useMemo(() => {
    const data = [...monitorPerformanceData];
    data.sort((a, b) => {
      const byName = String(a?.name || '').localeCompare(String(b?.name || ''), 'es', { sensitivity: 'base' });
      if (byName !== 0) {
        return monitorSortOrder === 'Z-A' ? -byName : byName;
      }

      // If monitors share name, keep a stable order by course.
      return String(a?.course || '').localeCompare(String(b?.course || ''), 'es', { sensitivity: 'base' });
    });
    return data;
  }, [monitorPerformanceData, monitorSortOrder]);

  const orderedMonitorsToShow = useMemo(() => {
    const uniqueNames = [];
    const seenNames = new Set();

    for (const item of sortedMonitorPerformanceData) {
      const name = item?.name;
      if (name && !seenNames.has(name)) {
        seenNames.add(name);
        uniqueNames.push(name);
      }
    }

    if (uniqueNames.length > 0) {
      return uniqueNames;
    }

    return sortMonitorsByOrder(monitorsToShow, monitorSortOrder);
  }, [sortedMonitorPerformanceData, monitorsToShow, monitorSortOrder]);

  const totalMonitorPages = Math.max(1, Math.ceil(sortedMonitorPerformanceData.length / monitorsPerPage));
  const paginatedMonitorPerformanceData = useMemo(() => {
    const start = (monitorPage - 1) * monitorsPerPage;
    const end = start + monitorsPerPage;
    return sortedMonitorPerformanceData.slice(start, end);
  }, [sortedMonitorPerformanceData, monitorPage]);

  useEffect(() => {
    if (monitorPage > totalMonitorPages) {
      setMonitorPage(totalMonitorPages);
    }
  }, [monitorPage, totalMonitorPages]);

  useEffect(() => {
    setMonitorPage(1);
  }, [semester, program, course, professor, monitor, monitorSortOrder]);

  useEffect(() => {
    if (monitor && !orderedMonitorsToShow.includes(monitor)) {
      setMonitor('');
    }
  }, [monitor, orderedMonitorsToShow]);
  console.log("BEFORE",monitorPerformanceData);

  //Porcentaje actividades monitores


  useEffect(() => {
    if (monitorPerformanceData.length > 0) {
  
      // Calcula los porcentajes globales para monitores
      let totalCompleted = 0;
      let totalLate = 0;
      let totalPending = 0;
  
      monitorPerformanceData.forEach(item => {
        totalCompleted += item.completed || 0;
        totalLate += item.late || 0;
        totalPending += item.pending || 0;
      });
  
      const total = totalCompleted + totalLate + totalPending;
  
      if (total > 0) {
        setCompletedPercent(`${Math.round((totalCompleted / total) * 100)}%`);
        setPendingPercent(`${Math.round((totalPending / total) * 100)}%`);
        setLatePercent(`${Math.round((totalLate / total) * 100)}%`);
        setPorcentages([{
          completed: `${Math.round((totalCompleted / total) * 100)}%`,
          pending: `${Math.round((totalPending / total) * 100)}%`,
          late: `${Math.round((totalLate / total) * 100)}%`
        }]);
      }
    } else {
      setCompletedPercent("0%");
      setPendingPercent("0%");
      setLatePercent("0%");
      setPorcentages([{ completed: "0%", late: "0%", pending: "0%" }]);
    }
  }, [monitorPerformanceData]);
  
  //Filter monitors information
  useEffect(() => {
    setMonitorPerformanceData(applyFilters(monitorPerformanceDataOriginal));
  }, [monitorPerformanceDataOriginal, semester, program, course, professor, monitor]);

  //Filter professor report 
  useEffect(() => {
    if (isProfessorRole && !ownProfessorName) {
      setProfessorData([]);
      return;
    }

    const data = Array.isArray(professorDataOriginal) ? professorDataOriginal : [];
    const report = data.filter(item =>
      (!course || item.course === course) &&
      (!effectiveProfessorFilter || item.name === effectiveProfessorFilter)
    );
    setProfessorData(report);
    
  }, [professorDataOriginal, course, effectiveProfessorFilter, isProfessorRole, ownProfessorName]); 


  //Porcentaje actividades profesores

useEffect(() => {
  if (professorData.length > 0) {
    let completed = 0;
    let late = 0;
    let pending = 0;
    
    professorData.forEach(item => {
      completed += item.completed || 0;
      late += item.late || 0;
      pending += item.pending || 0;
    });

    const total = completed + late + pending;

    if (total > 0) {
      const completedPercent = Math.round((completed / total) * 100);
      const latePercent = Math.round((late / total) * 100);
      const pendingPercent = Math.round((pending / total) * 100);

      setPorcentagesProfessor([{
        completed: `${completedPercent}%`,
        late: `${latePercent}%`,
        pending: `${pendingPercent}%`
      }]);

      setCompletedPercentProfessor(`${completedPercent}%`);
      setPendingPercentProfessor(`${pendingPercent}%`);
      setLatePercentProfessor(`${latePercent}%`);
    }
  } else {
    setCompletedPercentProfessor("0%");
    setPendingPercentProfessor("0%");
    setLatePercentProfessor("0%");
    setPorcentagesProfessor([{ completed: "0%", late: "0%", pending: "0%" }]);
  }
}, [professorData]);

  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setOpenHelpKey(null);
      }
    };

    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const renderCardTitle = (title, helpKey) => {
    const help = REPORT_HELP_CONTENT[helpKey];

    return (
      <div className="chart-card-header">
        <h3>{title}</h3>
        <div
          className="report-help-wrapper"
          onMouseEnter={() => setOpenHelpKey(helpKey)}
          onMouseLeave={() => setOpenHelpKey(null)}
          onFocusCapture={() => setOpenHelpKey(helpKey)}
          onBlurCapture={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) {
              setOpenHelpKey(null);
            }
          }}
        >
          <button
            type="button"
            className="report-help-icon"
            aria-label={`Ayuda de ${title}`}
            aria-expanded={openHelpKey === helpKey}
            onMouseEnter={() => setOpenHelpKey(helpKey)}
            onMouseLeave={() => setOpenHelpKey(null)}
            onFocus={() => setOpenHelpKey(helpKey)}
            onBlur={() => setOpenHelpKey(null)}
          >
            ?
          </button>

          {openHelpKey === helpKey && help && (
            <div className="report-help-tooltip" role="tooltip">
              <p>{help.summary}</p>
              <ul>
                {help.bullets.map((bullet) => (
                  <li key={bullet}>{bullet}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    );
  };


  const handleClose = () =>{
      setIsOpen(!isOpen)
      setChange(!change)
  }


  return (
    <div className="main">
      <PopUp
        show={isOpen}
        onClose={() => handleClose()}
      >{message}
      </PopUp>
      <div className="reports-top-bar">
        <h2 className="reports-title">Reportes</h2>
        <div className="filters-container">
          <div className="filter-group">
            <select onChange={(e) => setSemester(e.target.value)}>
                  <option value="">Semestre*</option>
              {sortedSemestersToShow.map((semester, index) => (
                      <option key={index} value={semester}>
                      {semester}
                      </option>
                  ))}
            </select>
          </div>
          <div className="filter-group">
            <select onChange={(e) => setProgram(e.target.value)}>
              <option value="">Programa*</option>
              {sortedProgramsToShow.map((program, index) => (
                    <option key={index} value={program}>
                    {program}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <select onChange={(e) => setCourse(e.target.value)}>
            <option value="">Curso*</option>
              {sortedCoursesToShow.map((course, index) => (
                    <option key={index} value={course}>
                    {course}
                    </option>
                ))}
            </select>
          </div>
          {!isProfessorRole && (
            <div className="filter-group">
              <select onChange={(e) => setProfessor(e.target.value)}>
              <option value="">Profesor</option>
                  {sortedProfessorsToShow.map((professor, index) => (
                      <option key={index} value={professor}>
                      {normalizeProfessorLabel(professor)}
                      </option>
                  ))}
              </select>
            </div>
          )}
        </div>
      </div>
      <div className="reports-container">
        <VerticalNavbar />
        <div className="reports-content">
          
    {/* Gráfico de barras - MONITOR */}
    <div className="chart-card">
      {renderCardTitle('Rendimiento de monitores', 'monitorPerformance')}
      <BarChart width={500} height={300} data={paginatedMonitorPerformanceData}>
        <CartesianGrid strokeDasharray="3 3" />
        
        {/* Mostrar solo el nombre del monitor */}
        <XAxis 
          dataKey="nameAndCourse" 
          tickFormatter={(value) => value.split(' ')[0]} 
        />
        
        <YAxis />
        
        {/* Tooltip */}
        <Tooltip 
          formatter={(value, name, props) => {
            const map = {
              completed: 'Completado',
              pending: 'Pendiente',
              late: 'Tarde',
            };

            const payload = props.payload;

            if (payload && typeof value === 'number') {
              const completed = payload.completed || 0;
              const pending = payload.pending || 0;
              const late = payload.late || 0;
              const total = completed + pending + late;

              const percentage = total > 0 ? Math.round((value / total) * 100) : 0;

              return [`${value} actividades - ${percentage}%`, map[name] || name];
            }

            return [`${value}`, map[name] || name];
          }}
          labelFormatter={(label) => `${label}`}
        />

        {/* Legend */}
        <Legend 
          formatter={(value) => {
            const map = {
              completed: 'Completado',
              pending: 'Pendiente',
              late: 'Tarde',
            };
            return map[value] || value;
          }} 
        />
        
        {/* Barra de Completado */}
        <Bar dataKey="completed" stackId="a" fill="#4cb979" />

        {/* Barra de Pendiente */}
        <Bar dataKey="pending" stackId="a" fill="#e9683b"/>

        {/* Barra de Tarde */}
        <Bar dataKey="late" stackId="a" fill="#e4eb60" />
           
      </BarChart>

      <div className="monitor-table-controls">
        <div className="filter-group">
          <select value={monitorSortOrder} onChange={(e) => setMonitorSortOrder(e.target.value)}>
            <option value="A-Z">Orden monitores: A - Z</option>
            <option value="Z-A">Orden monitores: Z - A</option>
          </select>
        </div>
      </div>

      <div className="monitor-table-wrapper">
        {orderedMonitorsToShow.length > 0 ? (
          <table className="monitor-report-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Monitores</th>
              </tr>
            </thead>
            <tbody>
              {orderedMonitorsToShow.map((monitorName, index) => (
                <tr key={`${monitorName}-${index}`}>
                  <td>{index + 1}</td>
                  <td>{monitorName}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="comparison-message">No hay monitores para los filtros seleccionados.</p>
        )}
      </div>

      {sortedMonitorPerformanceData.length > monitorsPerPage && (
        <div className="monitor-table-pagination">
          <span>Página {monitorPage} de {totalMonitorPages}</span>
          <div className="monitor-table-pagination-actions">
            <button
              className="chart-download-button"
              onClick={() => setMonitorPage(previous => Math.max(1, previous - 1))}
              disabled={monitorPage === 1}
            >
              Anterior
            </button>
            <button
              className="chart-download-button"
              onClick={() => setMonitorPage(previous => Math.min(totalMonitorPages, previous + 1))}
              disabled={monitorPage === totalMonitorPages}
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

          <div className="chart-download-container">
            <button className="chart-download-button" onClick={() => exportToCSV(sortedMonitorPerformanceData, 'Rendimiento_Monitores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>CSV</button>
            <button className="chart-download-button" onClick={() => exportToPDF(sortedMonitorPerformanceData, 'Rendimiento_Monitores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>PDF</button>
          </div>
        </div>

        <div className="chart-card">
          {renderCardTitle(
            semesterComparison.previousSemester
              ? `Comparativo por semestre (${semesterComparison.previousSemester} vs ${semesterComparison.currentSemester})`
              : 'Comparativo por semestre',
            'semesterComparison'
          )}

          {semesterComparison.previousSemester ? (
            <>
              <BarChart width={500} height={300} data={semesterComparison.chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="indicador" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Legend />
                <Bar dataKey={semesterComparison.previousSemester} fill="#e4eb60" />
                <Bar dataKey={semesterComparison.currentSemester} fill="#4cb979" />
              </BarChart>

              <div className="chart-download-container">
                <button className="chart-download-button" onClick={() => exportToCSV(semesterComparison.exportData, 'Comparativo_Semestres', {
                      semester,
                      program,
                      course,
                      professor,
                      monitor
                    })
                  }>CSV</button>
                <button className="chart-download-button" onClick={() => exportToPDF(semesterComparison.exportData, 'Comparativo_Semestres', {
                      semester,
                      program,
                      course,
                      professor,
                      monitor
                    })
                  }>PDF</button>
              </div>
            </>
          ) : (
            <p>No hay suficientes semestres con datos para comparar.</p>
          )}
        </div>


         {/* Porcentaje de tareas - MONITOR*/}
         <div className="chart-card">
          {renderCardTitle('Tareas completadas, tardias y pendientes de monitores', 'monitorTaskSummary')}
            <div className="reports-summary">
              <div className="summary-card completadas">
                <h4>Completadas</h4>
                <p>{completedPercent}</p>
              </div>
              <div className="summary-card tardias">
                <h4>Completadas tardías</h4>
                <p>{latePercent}</p>
              </div>
              <div className="summary-card pendientes">
                <h4>Pendientes</h4>
                <p>{pendingPercent}</p>
              </div>
            </div>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(porcentages, 'Resumen_Tareas_Monitor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(porcentages, 'Resumen_Tareas_Monitor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>PDF</button>
            </div>
          </div>

          {/* Gráfico de pastel*/}
          {course && (
            <div className="chart-card">
              {renderCardTitle(`Uso de Categorias - ${course}`, 'categoryUsage')}
               <PieChart width={400} height={300}>
                <Pie
                  data={pieChartData}
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  dataKey="cantidad_total"
                  nameKey="categoria"
                  label={({ categoria }) => categoria}
                >
                  {pieChartData.map((entry, index) => (
                    <Cell key={`cell-${index}-${entry.categoria}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  formatter={(value, name, props) => {
                    const total = pieChartData.reduce((acc, curr) => acc + curr.cantidad_total, 0);
                    const percent = ((value / total) * 100).toFixed(1);
                    return [`${value} actividades (${percent}%)`, 'Cantidad'];
                  }} 
                />
              </PieChart>
              <div className="chart-download-container">
                <button className="chart-download-button" onClick={() => exportToCSV(pieChartData, 'Categorias_Por_Curso', {
                    semester,
                    program,
                    course,
                    professor,
                    monitor
                  })
                }>CSV</button>
                <button className="chart-download-button" onClick={() => exportToPDF(pieChartData, 'Categorias_Por_Curso', {
                    semester,
                    program,
                    course,
                    professor,
                    monitor
                  })
                }>PDF</button>
              </div>
            </div>
          )}

        {/* Gráfico de barras - PROFESOR */}
        <div className="chart-card">
          {renderCardTitle(isProfessorRole ? 'Rendimiento del profesor' : 'Rendimiento de profesores', 'professorPerformance')}
            <BarChart width={500} height={300} data={professorChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              
              {/* Mostrar solo el nombre (jeje sin el apellido) en el eje X */}
              <XAxis 
                dataKey="nameAndCourse" 
                tickFormatter={(value) => value.split(' ')[0]} 
              />
              
              <YAxis />

              <Tooltip 
                formatter={(value, name, props) => {
                  const map = {
                    completed: 'Completado',
                    pending: 'Pendiente',
                    late: 'Tarde',
                  };

                  const payload = props.payload;

                  if (payload && typeof value === 'number') {
                    const completed = payload.completed || 0;
                    const pending = payload.pending || 0;
                    const late = payload.late || 0;
                    const total = completed + pending + late;

                    const percentage = total > 0 ? Math.round((value / total) * 100) : 0;

                    return [`${value} actividades - ${percentage}%`, map[name] || name];
                  }

                  return [`${value}`, map[name] || name];
                }}

                labelFormatter={(label) => {
                  const normalizedLabel = normalizeProfessorLabel(label);
                  const parts = label.split(' ');
                  if (parts.length >= 3) {
                    const nombreCompleto = `${parts[0]} ${parts[1]}`;
                    const nombreCurso = parts.slice(2).join(' ');
                    return normalizeProfessorLabel(`${nombreCompleto} ${nombreCurso}`);
                  }
                  return normalizedLabel; // fallback
                }}
              />



              <Legend 
                formatter={(value) => {
                  const map = {
                    completed: 'Completado',
                    pending: 'Pendiente',
                    late: 'Tarde',
                  };
                  return map[value] || value;
                }} 
              />

              <Bar dataKey="completed" stackId="a" fill="#4cb979" />
              <Bar dataKey="pending" stackId="a" fill="#e9683b" />
              <Bar dataKey="late" stackId="a" fill="#e4eb60" />
            </BarChart>

          <div className="chart-download-container">
            <button className="chart-download-button" onClick={() => exportToCSV(professorChartData, 'Rendimiento_Profesores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>CSV</button>
            <button className="chart-download-button" onClick={() => exportToPDF(professorChartData, 'Rendimiento_Profesores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>PDF</button>
          </div>
        </div>


         {/* Porcentaje de tareas - PROFESOR */}
         <div className="chart-card">
          {renderCardTitle('Tareas completadas, tardias y pendientes de profesores', 'professorTaskSummary')}
            <div className="reports-summary">
              <div className="summary-card completadas">
                <h4>Completadas</h4>
                <p>{completedPercentProfessor}</p>
              </div>
              <div className="summary-card tardias">
                <h4>Completadas tardías</h4>
                <p>{latePercentProfessor}</p>
              </div>
              <div className="summary-card pendientes">
                <h4>Pendientes</h4>
                <p>{pendingPercentProfessor}</p>
              </div>
            </div>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(porcentagesProfessor, 'Resumen_Tareas_Profesor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(porcentagesProfessor, 'Resumen_Tareas_Profesor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>PDF</button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

export default Reports;